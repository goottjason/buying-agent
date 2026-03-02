package com.sbshop.agent.infrastructure.external.smartstore.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.common.util.HtmlImageExtractor;
import com.sbshop.agent.infrastructure.external.smartstore.client.SmartstoreRestClient;
import com.sbshop.agent.infrastructure.external.smartstore.client.SmartstoreWebClient;
import com.sbshop.agent.infrastructure.external.smartstore.mapper.SmartstoreDataMapper;
import com.sbshop.agent.infrastructure.external.smartstore.parser.SmartstoreProductParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreSyncAdapter implements MarketSyncPort {

  // 통신, 파싱, 매핑, 유틸 전문가들을 모두 주입받습니다.
  private final SmartstoreRestClient smartstoreRestClient;
  private final SmartstoreProductParser productParser;
  private final SmartstoreDataMapper dataMapper;
  private final HtmlImageExtractor imageExtractor;
  private final ObjectMapper objectMapper; // rawData 변환용

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.SMARTSTORE;
  }

  @Override
  public List<String> fetchAllMarketItemIds() {
    List<String> allIds = new ArrayList<>();
    int page = 1;
    int size = 50; // 커머스 API 권장 사이즈
    boolean hasMore = true;

    log.info("🚀 [스마트스토어] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 🚀 1. GET 파라미터가 아니라 POST JSON 바디를 구성합니다.
        String path = "/v1/products/search";
        String requestBody = String.format("{\"page\": %d, \"size\": %d}", page, size);

        String responseJson = smartstoreRestClient.post(path, requestBody);
        JsonNode rootNode = objectMapper.readTree(responseJson);

        // API 응답 스펙에 따라 'contents' 혹은 'content' 일 수 있으니,
        // 혹시 데이터가 안 들어오면 이 문자열을 가장 먼저 의심해 보세요!
        JsonNode contentNode = rootNode.path("contents");

        if (contentNode.isMissingNode() || contentNode.isEmpty()) {
          hasMore = false;
        } else {
          for (JsonNode node : contentNode) {
            String originProductNo = node.path("originProductNo").asText("");
            // 🚀 빈 문자열("")까지 확실하게 방어
            if (!originProductNo.isBlank()) {
              allIds.add(originProductNo);
            }
          }
          page++; // 다음 페이지
          Thread.sleep(300);
        }
      } catch (Exception e) {
        log.error("❌ 스마트스토어 상품 목록 조회 중 오류 (page: {}): {}", page, e.getMessage());
        // 🚀 3. [핵심 방어벽] 여기서 빈 리스트를 반환하면 전체 꼬리표 참사가 또 일어납니다!
        // 무조건 예외를 던져서 PerfectSyncProcessor가 멈추게 만들어야 합니다.
        throw new RuntimeException("스마트스토어 API 장애로 인해 동기화를 강제 중단합니다.", e);
      }
    }
    log.info("📦 [스마트스토어] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketExtractedData extractProductData(String marketItemId) { // marketProductId = originProductNo
    // 1. 단건 상세 조회 엔드포인트 호출
    String path = "/v2/products/origin-products/" + marketItemId;
    String responseJson = smartstoreRestClient.get(path);

    try {
      // 2. 파서로 Root 진입
      JsonNode rootNode = productParser.parseRootNode(responseJson);
      JsonNode originProductNode = rootNode.path("originProduct");

      // 3. 전문가들을 통한 데이터 추출
      // 스마트스토어는 보통 sellerCustomCode1 에 우리의 자체 SKU를 넣습니다.
      String mappingKey = originProductNode.path("detailAttribute")
          .path("sellerCodeInfo")
          .path("sellerManagementCode").asText("");
      // String detailHtml = productParser.getTextFromOrigin(rootNode, "detailContent");

      // 🚀 지휘관(Processor)을 위한 매칭 열쇠! (우리의 자체 SKU)

      return MarketExtractedData.builder()
          .isMasterData(true)
          .mappingKey(mappingKey) // 🚀 핵심 열쇠 주입
          .marketIdentifiers(dataMapper.buildIdentifiers(marketItemId, rootNode))
          // .name(productParser.getTextFromOrigin(rootNode, "name"))
          // .salePrice(dataMapper.getSalePrice(rootNode))
          // .stock(dataMapper.getStockQuantity(rootNode))
          // .detailHtml(detailHtml)
          // 🚀 카페24에서 떼어낸 공통 유틸이 여기서 빛을 발합니다!
          // .images(imageExtractor.extractSkuImages(detailHtml, sku))
          .rawData(objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {}))
          .build();

    } catch (Exception e) {
      log.error("❌ 스마트스토어 상품 정보 추출 실패 (ID: {}): {}", marketItemId, e.getMessage());
      throw new RuntimeException("스마트스토어 데이터 추출 오류", e);
    }
  }

  @Override
  public boolean deleteMarketProduct(String marketItemId) {
    try {
      String path = "/v2/products/origin-products/" + marketItemId;
      smartstoreRestClient.delete(path);

      log.info("   🗑️ [스마트스토어] 유령 상품 완벽 삭제(DELETE) 완료 (ID: {})", marketItemId);
      return true;

    } catch (Exception e) {
      // 네이버 정책상 판매 이력이 있거나 승인된 상품은 삭제를 거부(400 Bad Request 등)할 확률이 높습니다.
      log.warn("   ⚠️ [스마트스토어] 유령 상품 삭제 거부됨 (ID: {}). 정책상 삭제 불가일 수 있습니다. 사유: {}", marketItemId, e.getMessage());

      // TODO: 만약 네이버가 삭제를 계속 튕겨낸다면, 쿠팡처럼 '판매 중지'로 우회하는 PUT API를 여기에 뚫어주세요!
        /*
        try {
            log.info("   👻 [스마트스토어] 삭제 대신 '판매 중지(SUSPENSION)' 우회 로직을 가동합니다.");
            String putPath = "/v1/products/" + marketItemId; // 또는 상태 변경 전용 엔드포인트
            // 상태를 SUSPENSION 이나 CLOSE 로 변경하는 JSON 바디 전송
            smartstoreRestClient.put(putPath, "{\"statusType\": \"SUSPENSION\"}");
            log.info("   🛑 [스마트스토어] 삭제 대신 판매 중지 처리 성공 (ID: {})", marketItemId);
            return true; // 로컬 DB 정리를 위해 true 반환
        } catch (Exception putEx) {
            log.error("   ❌ [스마트스토어] 판매 중지 우회 로직도 실패했습니다 (ID: {})", marketItemId, putEx);
        }
        */

      // 당장은 안전을 위해 false를 반환하여 로컬 DB에 찌꺼기를 일단 남겨둡니다.
      return false;
    }
  }
}
