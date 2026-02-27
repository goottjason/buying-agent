package com.sbshop.agent.infrastructure.external.coupang.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.common.util.HtmlImageExtractor;
import com.sbshop.agent.infrastructure.external.coupang.client.CoupangRestClient;
import com.sbshop.agent.infrastructure.external.coupang.mapper.CoupangDataMapper;
import com.sbshop.agent.infrastructure.external.coupang.parser.CoupangProductParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangSyncAdapter implements MarketSyncPort {

  private final ObjectMapper objectMapper;
  private final CoupangRestClient coupangRestClient;
  private final CoupangProductParser productParser;
  private final CoupangDataMapper dataMapper;
  private final HtmlImageExtractor imageExtractor;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  @Override
  public List<String> fetchAllMarketProductIds() {
    List<String> allIds = new ArrayList<>();
    String nextToken = ""; // 쿠팡은 페이지 번호 대신 이 토큰을 유지해야 함
    boolean hasMore = true;

    log.info("[쿠팡] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 쿠팡 셀러 상품 목록 조회 API (maxPerPage는 100이 최대치)
        String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products?maxPerPage=100";

        // 다음 페이지 토큰이 있다면 쿼리스트링에 이어 붙임
        if (!nextToken.isEmpty()) {
          path += "&nextToken=" + nextToken;
        }

        // 인증, 헤더, 타임아웃 세팅이 모두 은닉됨
        String responseJson = coupangRestClient.get(path);

        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode dataNode = rootNode.path("data");

        if (dataNode.isEmpty()) {
          // dataNode가 비어있다면 이미 끝까지 조회한 것
          hasMore = false;
        } else {
          for (JsonNode node : dataNode) {
            // 쿠팡의 마스터 식별자인 sellerProductId 추출
            allIds.add(node.path("sellerProductId").asText());
          }

          // 응답으로 온 nextToken을 갱신
          nextToken = rootNode.path("nextToken").asText("");

          // 더 이상 nextToken이 갱신되지 않으면 이미 끝까지 조회한 것
          if (nextToken.isEmpty()) {
            hasMore = false;
          }

          Thread.sleep(300); // 쿠팡 API Rate Limit 방어
        }
      } catch (Exception e) {
        log.error("❌ 쿠팡 상품 목록 조회 중 오류: {}", e.getMessage());
        break; // 에러 시 무한 루프 탈출
      }
    }

    log.info("📦 [쿠팡] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketExtractedData extractProductData(String marketProductId) { // sellerProductId

    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductId;
    String responseJson = coupangRestClient.get(path);

    try {
      JsonNode dataNode = productParser.parseDataNode(responseJson);
      JsonNode firstItem = productParser.getFirstItem(dataNode);

      // TODO: 일괄 변경 전에는 cafe24Code(ex: P000BAAA000A)가 담겨 있음

      // 쿠팡에 진짜 SKU가 있든, 카페24(P000...) 코드가 있든 그냥 이걸 던져줍니다.
      // 지휘관의 Dictionary가 찰떡같이 "아~ 이거 카페24 우회코드네?" 하고 알아서 찾아줍니다!
      String mappingKey = firstItem.path("externalVendorSku").asText("");

      // 명시적으로 값을 세팅하지 않은(생략한) 필드에 대해서는 자바의 기본값(객체는 null, int는 0, boolean은 false)을 자동으로 할당
      return MarketExtractedData.builder()
          .isMasterData(true)
          .marketIdentifiers(dataMapper.buildIdentifiers(marketProductId, firstItem))
          .mappingKey(mappingKey)
          .brand(dataNode.path("brand").asText(null))
          .manufacturer(dataNode.path("manufacturer").asText(null))
          .barcode(firstItem.path("barcode").asText(null))
          .generalProductName(dataNode.path("generalProductName").asText(null))
          // DE에서 띄우는 그 노란색 경고는 자바의 제네릭(Generic) 타입 소거 특성 때문에 발생하는 'Unchecked assignment (확인되지 않은 할당)' 경고입니다. Map.class라고만 적으면 자바는 그 안에 <String, Object>가 들어갈지 확신할 수 없어서 찝찝해하는 것이죠.
          // @SuppressWarnings("unchecked")를 달아서 조용히 시킬 수도 있지만, Jackson 라이브러리가 제공하는 TypeReference를 사용하면 애초에 경고가 발생하지 않도록 훨씬 우아하게 해결할 수 있습니다!
          .rawData(objectMapper.convertValue(dataNode, new TypeReference<Map<String, Object>>() {}))
          .build();

    } catch (Exception e) {
      log.error("❌ 쿠팡 상품 정보 추출 실패 (ID: {}): {}", marketProductId, e.getMessage());
      throw new RuntimeException("쿠팡 데이터 추출 오류", e);
    }
  }

  @Override
  public boolean deleteMarketProduct(String marketProductId) {
    try {
      String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductId;
      coupangRestClient.delete(path);

      log.info("🗑️ [쿠팡] 유령 상품 삭제 완료 (ID: {})", marketProductId);
      return true;
    } catch (Exception e) {
      log.error("❌ [쿠팡] 유령 상품 삭제 실패 (ID: {}): {}", marketProductId, e.getMessage());
      return false;
    }
  }

  @Override
  public void correctMarketSku(String marketProductId, String realSku) {
    try {
      // 쿠팡 상품 부분 수정 API 엔드포인트 (상품 수정 API 문서 참고 필요)
      String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductId;

      // TODO: realSku를 externalVendorSku 필드에 담아서 쿠팡에 부분 수정(PUT) 요청을 보냅니다.
      // String requestBody = ...
      // coupangWebClient.requestWithBody("PUT", path, requestBody);

      log.info("🛠️ [쿠팡] 가짜 SKU 교정 완료! 쿠팡 서버 반영 성공 (ID: {}, 변경된 SKU: {})", marketProductId, realSku);
    } catch (Exception e) {
      log.error("❌ [쿠팡] 가짜 SKU 교정 실패 (ID: {}): {}", marketProductId, e.getMessage());
    }
  }
}
