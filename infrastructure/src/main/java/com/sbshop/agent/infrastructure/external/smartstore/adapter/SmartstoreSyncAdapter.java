package com.sbshop.agent.infrastructure.external.smartstore.adapter;

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
  public List<String> fetchAllMarketProductIds() {
    List<String> allIds = new ArrayList<>();
    int page = 1;
    int size = 50; // 커머스 API 권장 사이즈
    boolean hasMore = true;

    log.info("🚀 [스마트스토어] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 스마트스토어 커머스 API 상품 검색 엔드포인트
        String path = "/v1/products/search?page=" + page + "&size=" + size;

        String responseJson = smartstoreRestClient.get(path);
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode contentNode = rootNode.path("contents");

        if (contentNode.isEmpty()) {
          hasMore = false;
        } else {
          for (JsonNode node : contentNode) {
            String originProductNo = node.path("originProductNo").asText();
            if (originProductNo != null && !originProductNo.isBlank()) {
              allIds.add(originProductNo);
            }
          }
          page++; // 다음 페이지
          Thread.sleep(300);
        }
      } catch (Exception e) {
        log.error("❌ 스마트스토어 상품 목록 조회 중 오류 (page: {}): {}", page, e.getMessage());
        break;
      }
    }
    log.info("📦 [스마트스토어] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketExtractedData extractProductData(String marketProductId) { // marketProductId = originProductNo
    // 1. 단건 상세 조회 엔드포인트 호출
    String path = "/v1/products/" + marketProductId;
    String responseJson = smartstoreRestClient.get(path);

    try {
      // 2. 파서로 Root 진입
      JsonNode rootNode = productParser.parseRootNode(responseJson);

      // 3. 전문가들을 통한 데이터 추출
      // 스마트스토어는 보통 sellerCustomCode1 에 우리의 자체 SKU를 넣습니다.
      String sku = productParser.getTextFromOrigin(rootNode, "sellerCustomCode1");
      String detailHtml = productParser.getTextFromOrigin(rootNode, "detailContent");

      // 🚀 지휘관(Processor)을 위한 매칭 열쇠! (우리의 자체 SKU)
      String mappingKey = sku;

      return MarketExtractedData.builder()
          .isMasterData(true)
          .mappingKey(mappingKey) // 🚀 핵심 열쇠 주입
          .marketIdentifiers(dataMapper.buildIdentifiers(marketProductId, rootNode))
          .name(productParser.getTextFromOrigin(rootNode, "name"))
          .salePrice(dataMapper.getSalePrice(rootNode))
          .stock(dataMapper.getStockQuantity(rootNode))
          .detailHtml(detailHtml)
          // 🚀 카페24에서 떼어낸 공통 유틸이 여기서 빛을 발합니다!
          .images(imageExtractor.extractSkuImages(detailHtml, sku))
          .rawData(objectMapper.convertValue(rootNode, Map.class))
          .build();

    } catch (Exception e) {
      log.error("❌ 스마트스토어 상품 정보 추출 실패 (ID: {}): {}", marketProductId, e.getMessage());
      throw new RuntimeException("스마트스토어 데이터 추출 오류", e);
    }
  }

  @Override
  public boolean deleteMarketProduct(String marketProductId) {
    try {
      String path = "/v1/products/" + marketProductId;
      smartstoreRestClient.delete(path);

      log.info("🗑️ [스마트스토어] 유령 상품 삭제 완료 (ID: {})", marketProductId);
      return true;
    } catch (Exception e) {
      log.error("❌ [스마트스토어] 유령 상품 삭제 실패 (ID: {}): {}", marketProductId, e.getMessage());
      return false;
    }
  }
}
