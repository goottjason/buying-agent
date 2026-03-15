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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
    if (rawData == null || rawData.isEmpty()) {
      return MarketItemInfo.builder().build();
    }

    // =====================================================================
    // 1. 최상단 데이터 (상품명, SKU, 가격) 추출
    // =====================================================================
    String productName = rawData.get("product_name") != null ? String.valueOf(rawData.get("product_name")) : null;
    String customProductCode = rawData.get("custom_product_code") != null ? String.valueOf(rawData.get("custom_product_code")) : "";

    BigDecimal salePrice = null;
    if (rawData.get("price") != null) {
      try {
        // 카페24는 "99000.00" 처럼 소수점이 포함된 문자열로 가격을 줍니다. BigDecimal이 이를 완벽하게 파싱합니다.
        salePrice = new BigDecimal(String.valueOf(rawData.get("price")));
      } catch (NumberFormatException e) {
        log.warn("카페24 가격 데이터 파싱 실패: {}", rawData.get("price"));
      }
    }

    // =====================================================================
    // 2. 내부 variants 배열 (재고) 추출
    // 카페24는 옵션별 재고를 관리하므로, variants 배열의 첫 번째 옵션 재고를 가져옵니다.
    // =====================================================================
    Integer stock = 0;
    try {
      Object variantsObj = rawData.get("variants");
      if (variantsObj instanceof java.util.List) {
        java.util.List<?> variants = (java.util.List<?>) variantsObj;
        if (!variants.isEmpty()) {
          @SuppressWarnings("unchecked")
          Map<String, Object> firstVariant = (Map<String, Object>) variants.get(0);

          if (firstVariant.get("quantity") != null) {
            // 소수점이 있을 경우를 대비해 Double로 먼저 파싱 후 int로 변환하는 것이 가장 안전합니다.
            stock = (int) Double.parseDouble(String.valueOf(firstVariant.get("quantity")));
          }
        }
      }
    } catch (Exception e) {
      log.warn("카페24 로컬 데이터 내부 variants 배열 파싱 실패", e);
    }

    // =====================================================================
    // 3. 조립 및 반환
    // =====================================================================
    return MarketItemInfo.builder()
        .isMasterData(true)
        .name(productName)
        .mappingKey(customProductCode)
        // 카페24 JSON에는 명시적인 brand 텍스트가 없으므로 생략 (필요 시 summary_description 등 활용 가능)
        .salePrice(salePrice)
        .stock(stock)
        .rawData(rawData)
        .build();
  }

  @Override
  public Map<String, Object> syncPriceAndStock(String marketItemId, Map<String, Object> currentRawData, Integer price, Integer stock) {

    // 🚀 1. 실제 카페24 API 호출 로직
    // cafe24RestClient.put("/api/v2/products/" + marketItemId, updateDto);

    // 🚀 2. 로컬 Map 패치
    try {
      if (currentRawData != null) {
        // 2-1. 최상단 가격 업데이트 (카페24 스펙에 맞춰 소수점 ".00" 붙이기)
        if (price != null) {
          currentRawData.put("price", price + ".00");
        }

        // 2-2. 내부 옵션 배열(variants)의 재고 업데이트
        if (stock != null && currentRawData.containsKey("variants")) {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> variants = (List<Map<String, Object>>) currentRawData.get("variants");

          if (variants != null && !variants.isEmpty()) {
            Map<String, Object> firstVariant = variants.get(0);
            firstVariant.put("quantity", stock);
          }
        }
      }
    } catch (Exception e) {
      log.warn("카페24 로컬 Map 데이터 패치 중 오류 발생", e);
    }

    return currentRawData;
  }

  @Override
  public Map<String, Object> syncImagesAndHtml(String marketItemId, Map<String, Object> currentRawData, List<String> hostedImages, String newDetailHtml) {

    // =====================================================================
    // 1. 카페24 전송용 Request Body 조립 (바꿀 항목만 전송)
    // =====================================================================
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> productData = new HashMap<>();
    productData.put("shop_no", 1); // 기본 샵 번호

    // 🚀 [핵심 추가] 카페24 규격: "내가 보내는 건 URL(A) 타입 이미지야!" 라고 명시
    productData.put("image_upload_type", "A");

    // 대표 이미지 교체 (첫 번째 이미지)
    if (hostedImages != null && !hostedImages.isEmpty()) {
      productData.put("detail_image", hostedImages.get(0));
      // 추가 이미지가 있다면 카페24 스펙에 맞춰 추가 (버전에 따라 additional_image 리스트로 처리)
    }

    // HTML 교체
    productData.put("description", newDetailHtml);
    requestBody.put("request", productData);

    // 🚀 2. API 통신 로직
    cafe24RestClient.put("/admin/products/" + marketItemId, requestBody);
    log.info("카페24 이미지/HTML 동기화 완료: {}", marketItemId);

    // =====================================================================
    // 3. 로컬 데이터 패치 (UI 반영용)
    // =====================================================================
    if (currentRawData != null) {
      if (hostedImages != null && !hostedImages.isEmpty()) {
        currentRawData.put("detail_image", hostedImages.get(0));
      }
      currentRawData.put("description", newDetailHtml);
    }

    return currentRawData;
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
