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
import java.util.UUID;
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

  @Override
  public Map<String, String> publish(Product product) {
    // 1. Product -> Cafe24ProductDto 로 변환
    // 2. OAuth2 토큰 셋팅
    // 3. POST https://{mall_id}.cafe24api.com/api/v2/admin/products
    // 4. 응답 파싱 후 카페24 상품 번호 반환


    log.info("🛒 [카페24] API 연동 시작... 대상 상품: {}", product.getName());

    // TODO: 내일 여기서 Product -> Cafe24Dto 변환 및 실제 RestTemplate HTTP 통신 진행!

    log.info("🛒 [카페24] API 연동 완료!");
    Map<String, String> identifiers = new HashMap<>();
    identifiers.put("product_no", "C24-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    // 오늘은 가짜 식별자를 리턴합니다. (실제로는 "P000000W" 같은 카페24 발급 코드가 됨)
    return identifiers;
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
    // 1. 카페24 상세설명 업데이트 (PUT)
    // =====================================================================
    Map<String, Object> descriptionRequestBody = new HashMap<>();
    Map<String, Object> descriptionData = new HashMap<>();
    descriptionData.put("shop_no", 1);
    descriptionData.put("description", newDetailHtml);
    // 외부 이미지를 사용한다는 설정을 활성화 (이미지 업로드 전 선행 작업)
    if (hostedImages != null && !hostedImages.isEmpty()) {
       descriptionData.put("use_external_image", "T");
    }
    descriptionRequestBody.put("request", descriptionData);

    try {
      cafe24RestClient.put("/admin/products/" + marketItemId, descriptionRequestBody);
      log.info("✅ [카페24] 상세설명(HTML) 및 외부이미지 설정 업데이트 완료: {}", marketItemId);
    } catch (Exception e) {
      log.error("❌ [카페24] 상세설명 업데이트 실패 (ID: {}): {}", marketItemId, e.getMessage());
    }

    // =====================================================================
    // 2. 카페24 이미지 업데이트 (Base64 방식)
    // =====================================================================
    if (hostedImages != null && !hostedImages.isEmpty()) {
      try {
        // [2-1] 기존 이미지 삭제 (초기화 후 새로 등록하여 대표 이미지 교체 보장)
        try {
          cafe24RestClient.delete("/admin/products/" + marketItemId + "/images");
          log.info("🗑️ [카페24] 기존 상품 이미지 초기화 완료: {}", marketItemId);
        } catch (Exception e) {
          log.warn("⚠️ [카페24] 기존 이미지 삭제 중 경고 (이미지가 없는 경우 발생 가능): {}", e.getMessage());
        }

        // [2-2] 이미지 다운로드 및 Base64 변환
        String mainImageUrl = hostedImages.get(0);
        byte[] imageBytes = cafe24RestClient.getExternalImageBytes(mainImageUrl);
        
        if (imageBytes != null) {
          String base64Content = java.util.Base64.getEncoder().encodeToString(imageBytes);
          String dataUri = "data:image/jpeg;base64," + base64Content;

          Map<String, Object> imageRequestBody = new HashMap<>();
          Map<String, Object> imageData = new HashMap<>();
          
          imageData.put("shop_no", 1);
          imageData.put("image_upload_type", "B"); // Base64 데이터 스트림 방식
          imageData.put("detail_image", dataUri);
          imageData.put("list_image", dataUri);
          imageData.put("tiny_image", dataUri);
          imageData.put("small_image", dataUri);
          
          imageRequestBody.put("request", imageData);

          // [2-3] POST 전송
          cafe24RestClient.post("/admin/products/" + marketItemId + "/images", imageRequestBody);
          log.info("✅ [카페24] 상품 이미지 Base64 업로드 및 교체 완료: {}", marketItemId);
        }
      } catch (Exception e) {
        log.error("❌ [카페24] 상품 이미지 업데이트 실패 (ID: {}): {}", marketItemId, e.getMessage());
      }
    }

    // =====================================================================
    // 3. 로컬 데이터 패치
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
