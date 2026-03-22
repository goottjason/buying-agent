package com.sbshop.agent.infrastructure.client.coupang.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.infrastructure.client.coupang.client.CoupangRestClient;
import com.sbshop.agent.infrastructure.client.coupang.dto.CategoryMetaResult;
import com.sbshop.agent.infrastructure.client.coupang.dto.CoupangProductPayload.Item.Attribute;
import com.sbshop.agent.infrastructure.client.coupang.dto.CoupangProductPayload.Item.Notice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoupangMetaService {

  private final CoupangRestClient restClient;
  private final ObjectMapper objectMapper;

  /**
   * 🚀 쿠팡 카테고리 메타 API 조회 및 캐싱
   * @Cacheable: 최초 1회만 API를 찌르고, 이후 7일간은 Redis에서 바로 꺼내옴! (속도 폭발, 한도 방어)
   *
   * [💡 해결책 및 주의사항 안내]
   * 파라미터 불일치 에러를 해결하기 위해 getCategoryMeta에 Product 파라미터를 추가하고 
   * extractMandatoryAttributes로 전달하도록 수정했습니다.
   * 
   * 🚨 하지만 현재 상태로는 심각한 캐싱 버그가 발생할 수 있습니다.
   * - 문제점: @Cacheable의 키가 categoryId이므로, 첫 번째 상품(Product)을 기준으로 계산된 속성(용량/수량)이 캐싱됩니다.
   *   결과적으로 같은 카테고리의 다른 상품들도 모두 첫 번째 상품의 용량/수량 값으로 등록되는 문제가 발생합니다.
   * - 올바른 해결책: 
   *   1. 캐싱 대상 분리: 쿠팡 API와 통신하여 원본 JSON 데이터(메타정보)를 가져오는 로직만 별도 메서드나 클래스로 분리하여 캐싱합니다.
   *   2. 동적 조립: getCategoryMeta()에서는 캐시에서 원본 메타정보만 꺼내오고, 파라미터로 받은 Product 정보를 조합해 매번 새롭게 extractMandatoryAttributes()를 수행하도록 구조를 리팩토링해야 합니다.
   *   (내부 메서드 호출 시 Spring AOP 프록시를 타지 않아 캐시가 무시되는 Self-invocation 현상을 주의하여 별도 컴포넌트로 분리하는 것을 권장합니다.)
   */
  @Cacheable(value = "coupangCategoryMeta", key = "#categoryId")
  public CategoryMetaResult getCategoryMeta(Long categoryId, Product product) throws Exception {
    log.info("[Coupang Meta API] Redis 캐시에 없어서 외부 API를 호출합니다. CategoryId: {}", categoryId);

    String path = "/v2/providers/openapi/apis/api/v2/products/category-meta/" + categoryId;
    String response = restClient.requestWithBody("GET", path, null);
    JsonNode dataNode = objectMapper.readTree(response).path("data");

    // 1. 필수 옵션(Attributes) 방어 로직 조립
    List<Attribute> attributes = extractMandatoryAttributes(dataNode, product);

    // 2. 고시정보(Notices) 방어 로직 조립
    List<Notice> notices = extractNotices(dataNode);

    return CategoryMetaResult.builder()
        .attributes(attributes)
        .notices(notices)
        .build();
  }

  // =========================================================================
  // 🛡️ [마법 1] 필수 속성(MANDATORY)만 골라내서 값 때우기 로직
  // =========================================================================
  // 🚀 메서드 시그니처 변경: Product를 받아와서 실제 용량/수량을 계산합니다!
  private List<Attribute> extractMandatoryAttributes(JsonNode dataNode, Product product) {
    List<Attribute> attributes = new ArrayList<>();
    JsonNode attributesNode = dataNode.path("attributes");

    for (JsonNode attr : attributesNode) {
      // 🚨 쿠팡이 필수로 요구하는(MANDATORY) 항목만 뽑아냄!
      if ("MANDATORY".equals(attr.path("basicRequired").asText())) {
        String typeName = attr.path("attributeTypeName").asText();
        String dataType = attr.path("dataType").asText();
        JsonNode unitsNode = attr.path("usableUnits");

        String valueName = "상세페이지 참조"; // 기본 방어 텍스트

        // ==========================================================
        // 🎯 [정밀 타격] 숫자(NUMBER) 타입일 경우 실제 데이터를 조립!
        // ==========================================================
        if ("NUMBER".equals(dataType)) {

          // 1. 값(Value) 추출: "수량" 관련 속성이면 묶음 계산, 아니면 용량(Capacity)
          if (typeName.contains("수량") || typeName.contains("캡슐") || typeName.contains("정")) {
            // 영양제 총 수량 계산: 90정 * 2개 = 180
            int bundleQty = product.getLogisticsInfo() != null ? product.getLogisticsInfo().getBundleQuantity() : 1;
            int totalCount = product.getProductSpec().getCapacity().intValue() * bundleQty;
            valueName = String.valueOf(totalCount > 0 ? totalCount : 1);
          }
          else if (typeName.contains("용량") || typeName.contains("중량") || typeName.contains("함량")) {
            // 1정당 용량 (예: 500mg)
            valueName = String.valueOf(product.getProductSpec().getCapacity().intValue() > 0 ? product.getProductSpec().getCapacity() : 1);
          }
          else {
            // 뭔지 모르겠으면 최후의 생존값 "1"
            valueName = "1";
          }

          // 2. 단위(Unit) 추출: 쿠팡 허용 목록과 내 단위를 교차 검증!
          if (unitsNode.isArray() && !unitsNode.isEmpty()) {
            String matchedUnit = findProperUnit(unitsNode, String.valueOf(product.getProductSpec().getMeasureUnit()));
            valueName += matchedUnit; // 최종 조립 (예: "180" + "정")
          }
        }

        attributes.add(Attribute.builder()
            .attributeTypeName(typeName)
            .attributeValueName(valueName)
            .exposed("NONE") // 검색 필터 노출 안함
            .build());
      }
    }
    return attributes;
  }

  /**
   * 🛡️ 쿠팡이 허용한 단위(usableUnits) 중 가장 적절한 단위를 찾아주는 헬퍼
   */
  private String findProperUnit(JsonNode usableUnitsNode, String myUnit) {
    if (myUnit == null || myUnit.isBlank()) {
      return usableUnitsNode.get(0).asText(); // 내 단위가 없으면 쿠팡 첫 번째 단위 강제 사용
    }

    // 1. 완전 일치 또는 포함되는 단위 찾기 (예: 내가 "타블렛"인데 쿠팡에 "정"이 있으면 매칭해야 함)
    // 실무에서는 영양제 단위 동의어 사전을 맵핑합니다.
    String normalizedMyUnit = normalizeUnit(myUnit);

    for (JsonNode unitNode : usableUnitsNode) {
      String coupangUnit = unitNode.asText();
      if (coupangUnit.contains(normalizedMyUnit) || normalizedMyUnit.contains(coupangUnit)) {
        return coupangUnit; // 쿠팡이 준 정확한 텍스트로 리턴! (절대 내 마음대로 적지 않음)
      }
    }

    // 2. 못 찾았으면 에러 뱉지 말고 그냥 쿠팡이 준 첫 번째 단위로 덮어쓰기 (생존!)
    return usableUnitsNode.get(0).asText();
  }

  /**
   * 💡 영양제 단위 동의어 정규화 (실무 꿀팁)
   */
  private String normalizeUnit(String unit) {
    if (unit.contains("타블렛") || unit.contains("tablet") || unit.contains("tab")) return "정";
    if (unit.contains("캡슐") || unit.contains("capsule") || unit.contains("cap")) return "캡슐";
    if (unit.contains("소프트겔") || unit.contains("softgel")) return "캡슐";
    return unit; // g, mg, ml 등은 그대로 패스
  }

  // =========================================================================
  // 🛡️ [마법 2] 고시정보 항목 싹 다 "상세페이지 참조"로 도배하기
  // =========================================================================
  private List<Notice> extractNotices(JsonNode dataNode) {
    List<Notice> notices = new ArrayList<>();

    // 보통 첫 번째 고시정보 템플릿(예: 건강기능식품)을 사용함
    JsonNode firstNoticeCategory = dataNode.path("noticeCategories").get(0);
    if (firstNoticeCategory != null) {
      String noticeName = firstNoticeCategory.path("noticeCategoryName").asText();
      JsonNode detailNames = firstNoticeCategory.path("noticeCategoryDetailNames");

      for (JsonNode detail : detailNames) {
        String detailName = detail.path("noticeCategoryDetailName").asText();

        notices.add(Notice.builder()
            .noticeCategoryName(noticeName)
            .noticeCategoryDetailName(detailName)
            .content("상품상세페이지 참조") // 🚀 셀러들의 생명줄 "상세참조" 빔 발사!
            .build());
      }
    }
    return notices;
  }
}