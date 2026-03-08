package com.sbshop.agent.infrastructure.client.cafe24.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import com.sbshop.agent.infrastructure.client.cafe24.client.Cafe24RestClient;
import com.sbshop.agent.infrastructure.client.cafe24.mapper.Cafe24DataMapper;
import com.sbshop.agent.infrastructure.client.cafe24.parser.Cafe24ProductParser;
import com.sbshop.agent.infrastructure.client.common.util.HtmlImageExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24MarketClient implements MarketClient {

  private final ObjectMapper objectMapper;
  private final Cafe24RestClient cafe24RestClient;
  private final Cafe24ProductParser productParser;
  private final Cafe24DataMapper dataMapper;
  private final HtmlImageExtractor imageExtractor;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.CAFE24;
  }

  public List<String> fetchAllMarketItemIds() {
    List<String> allIds = new ArrayList<>();
    int limit = 100; // 카페24 최대 허용치
    int offset = 0;
    boolean hasMore = true;

    log.info("🚀 [카페24] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        String path = "/admin/products?limit=" + limit + "&offset=" + offset + "&fields=product_no";

        String responseJson = cafe24RestClient.get(path);
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode productsNode = rootNode.path("products");

        if (productsNode.isEmpty()) {
          hasMore = false; // 더 이상 상품이 없으면 탈출!
        } else {
          for (JsonNode node : productsNode) {
            allIds.add(node.path("product_no").asText());
          }
          offset += limit; // 다음 페이지를 위해 offset 증가
          Thread.sleep(300); // API Rate Limit 방어
        }
      } catch (Exception e) {
        log.error("❌ 카페24 상품 목록 조회 중 오류 (offset: {}): {}", offset, e.getMessage());
        break; // 에러 시 무한루프 방지
      }
    }
    log.info("📦 [카페24] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketItemInfo extractMarketItem(String marketItemId) {
    String path = "/admin/products/" + marketItemId + "?embed=variants";
    String responseJson = cafe24RestClient.get(path);

    try {
      // 1. 파서로 구조 진입
      JsonNode productNode = productParser.parseProductNode(responseJson);

      // 2. 매퍼와 유틸을 이용한 데이터 추출
      String sku = productParser.getText(productNode, "custom_product_code");
      String detailHtml = dataMapper.getMergedDescription(productNode);

      // 🚀 지휘관(Processor)을 위한 매칭 열쇠: product_code (P000...)
      String mappingKey = productParser.getText(productNode, "product_code");

      return MarketItemInfo.builder()
          .isMasterData(true)
          .mappingKey(mappingKey)
          .marketIdentifiers(dataMapper.buildIdentifiers(marketItemId, productNode))
          .name(productParser.getText(productNode, "product_name"))
          .originalName(productParser.getText(productNode, "eng_product_name"))
          .salePrice(dataMapper.getPrice(productNode))
          .stock(dataMapper.calculateTotalStock(productNode))
          .detailHtml(detailHtml)
          .images(imageExtractor.extractSkuImages(detailHtml, sku))
          .rawData(objectMapper.convertValue(productNode, Map.class)) // 원본 데이터 보존
          .build();

    } catch (Exception e) {
      log.error("❌ 카페24 상품 정보 추출 실패 (ID: {}): {}", marketItemId, e.getMessage());
      throw new RuntimeException("카페24 데이터 추출 오류", e);
    }
  }

  @Override
  public MarketItemInfo parseLocalData(Map<String, Object> rawData) {
    return null;
  }

  @Override
  public Map<String, Object> syncPriceAndStock(String marketItemId, Map<String, Object> currentRawData, Integer price, Integer stock) {
    return Map.of();
  }

  @Override
  public Map<String, Object> syncImagesAndHtml(String marketItemId, Map<String, Object> currentRawData, List<String> hostedImages, String newDetailHtml) {
    return Map.of();
  }

  public boolean deleteMarketProduct(String marketItemId) {
    try {
      String path = "/admin/products/" + marketItemId;
      cafe24RestClient.delete(path);

      log.info("🗑️ [카페24] 유령 상품 삭제 완료 (ID: {})", marketItemId);
      return true;
    } catch (Exception e) {
      log.error("❌ [카페24] 유령 상품 삭제 실패 (ID: {}): {}", marketItemId, e.getMessage());
      return false;
    }
  }

  public void updateProductImageAndHtml(Map<String, String> identifiers, Product product) {

  }
}
