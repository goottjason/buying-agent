package com.sbshop.agent.infrastructure.external.cafe24.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.cafe24.client.Cafe24WebClient;
import com.sbshop.agent.infrastructure.external.cafe24.config.Cafe24Properties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24ProductAdapter implements
    MarketProductReaderPort,
    MarketDataExtractorPort,
    MarketCommandPort {

  private final Cafe24Properties properties;
  private final Cafe24WebClient webClient;
  private final ObjectMapper objectMapper;

  // 팩토리가 여러 어댑터 중에서 나를 찾을 수 있게 해주는 "명찰"
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.CAFE24;
  }

  /*public Optional<MarketExtractedData> getProductDetailsBySku(String sku) {
    // 1. embed까지 써서 한 방에 찌릅니다!
    String path = "/admin/products?internal_product_name=" + sku + "&embed=variants";
    String responseJson = webClient.get(path);
    try {
      Map<String, Object> responseMap = objectMapper.readValue(
          responseJson,
          new TypeReference<>() {}
      );

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> products = (List<Map<String, Object>>) responseMap.get("products");

      // 검색 결과가 있다면 첫 번째 상품의 덩어리를 통째로 가져옵니다.
      if (products != null && !products.isEmpty()) {
        Map<String, Object> productNode = products.getFirst();

        // 2. 바로 파싱해서 리턴
        return Optional.of(MarketExtractedData.builder()
            .detailHtml("")
            .images(new ArrayList<>())
            .rawData(productNode) // 모든 데이터를 바구니에 담음
            .build());
      }
    } catch (Exception e) {
      log.error("카페24 SKU 한방 조회 실패", e);
    }
    return Optional.empty();
  }*/

  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {

    // 자체상품코드(custom_product_code)로 검색하고, 응답 데이터 다이어트를 위해 product_no 필드만 요청합니다.
    String path = "/admin/products?custom_product_code=" + sku + "&fields=product_no";

    String responseJson = webClient.get(path);

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode productsNode = root.path("products");

      // 배열에 결과가 1개라도 있다면 첫 번째 상품의 번호를 가져옵니다.
      if (productsNode.isArray() && !productsNode.isEmpty()) {
        return Optional.of(productsNode.get(0).path("product_no").asText());
      } else {
        // 🚀 수정 포인트 2: 못 찾았을 때 원본 응답을 찍어보는 디버깅 로그 추가
        log.warn("🔍 [검색 실패] SKU: {} -> 카페24 응답: {}", sku, responseJson);
      }
    } catch (Exception e) {
      log.error("카페24 SKU({}) 검색 파싱 실패: {}", sku, e.getMessage());
    }

    // 검색 결과가 없거나 에러가 나면 빈 껍데기를 반환합니다.
    return Optional.empty();
  }
  @Override
  public MarketExtractedData extractInitialProductData(String marketProductNo) {
    // 기존 getProductDetailsByMarketProductNo 로직 (HTML 정규식 파싱 등 무거운 작업)
    // 1. GET 요청 쏘기
    String responseJson = webClient.get("/admin/products/" + marketProductNo + "?embed=variants");

    try {
      // 1. JSON 문자열을 Map으로 파싱
      Map<String, Object> responseMap = objectMapper.readValue(
          responseJson,
          new TypeReference<Map<String, Object>>() {}
      );

      // 2. 카페24 API 응답은 보통 {"product": { 실제 데이터... }} 형태이므로 알맹이만 꺼냅니다.
      @SuppressWarnings("unchecked")
      Map<String, Object> productNode = (Map<String, Object>) responseMap.get("product");

      // =====================================================================
      // 1. 안전한 데이터 추출 (null 방어)
      // =====================================================================
      Object skuObj = productNode.get("custom_product_code");
      String sku = skuObj != null ? skuObj.toString().trim() : "";

      // PC 상세설명을 먼저 찾고, 비어있으면 모바일 상세설명까지 싹 뒤집니다.
      Object descObj = productNode.get("description");
      String detailHtml = descObj != null ? descObj.toString() : "";

      if (detailHtml.isBlank()) {
        Object mobileDescObj = productNode.get("mobile_description");
        detailHtml = mobileDescObj != null ? mobileDescObj.toString() : "";
      }

      // 🔍 [디버깅 1] HTML 데이터가 실제로 존재하는지 로그로 확인!
      log.info("🔍 [HTML 파싱 확인] SKU: {}, HTML 길이: {} bytes", sku, detailHtml.length());


      // =====================================================================
      // 🚀 2. 대소문자 완벽 대응 정규식 & 이미지 파싱
      // =====================================================================
      List<String> images = new java.util.ArrayList<>();
      if (!detailHtml.isBlank() && !sku.isBlank()) {
        // Pattern.CASE_INSENSITIVE 를 추가하여 <IMG SRC=> 등 대소문자 섞임 방어
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(detailHtml);

        String lowerSku = sku.toLowerCase(); // 비교를 위해 SKU도 소문자로 통일

        while (matcher.find()) {
          String imgUrl = matcher.group(1);
          // URL 내부에도 대소문자가 섞여있을 수 있으므로 모두 소문자로 변환 후 검사
          if (imgUrl.toLowerCase().contains(lowerSku)) {
            images.add(imgUrl);
          }
        }
        // 알파벳/숫자 오름차순 정렬 (sku-1.jpg, sku-2.jpg 순서 완벽 보장)
        java.util.Collections.sort(images);
      }

      // 🔍 [디버깅 2] 정규식이 이미지를 몇 개나 잡아냈는지 로그로 확인!
      log.info("📸 [이미지 추출 결과] 총 {}장 파싱됨: {}", images.size(), images);

      // =====================================================================
      // 3. 재고 & 가격 파싱 (안전 처리 유지)
      // =====================================================================
      int stock = 0;
      if (productNode.containsKey("variants")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> variants = (List<Map<String, Object>>) productNode.get("variants");
        if (variants != null && !variants.isEmpty()) {
          Object quantityObj = variants.getFirst().get("quantity");
          stock = quantityObj != null ? Integer.parseInt(quantityObj.toString()) : 0;
        }
      }

      Object priceObj = productNode.get("price");
      java.math.BigDecimal salePrice = (priceObj != null && !priceObj.toString().isBlank())
          ? new java.math.BigDecimal(priceObj.toString())
          : java.math.BigDecimal.ZERO;

      Object nameObj = productNode.get("product_name");
      Object engNameObj = productNode.get("eng_product_name");

      // =========================================================================
      // 🚀 식별자(Identifiers) 바구니 조립: product_no + product_code + custom_product_code
      // =========================================================================
      Map<String, String> marketIdentifiers = new java.util.HashMap<>();

      // 1. 기본 식별자: API 통신용 상품 고유 번호 (숫자형태 문자열)
      marketIdentifiers.put("product_no", marketProductNo);

      // 2. 추가 식별자 1: 쇼핑몰 프론트 노출용 상품 코드 (예: P00000RQ)
      Object productCodeObj = productNode.get("product_code");
      if (productCodeObj != null && !productCodeObj.toString().isBlank()) {
        marketIdentifiers.put("product_code", productCodeObj.toString());
      }

      // 3. 추가 식별자 2: 자체상품코드 (교차 검증용 SKU)
      Object customCodeObj = productNode.get("custom_product_code");
      if (customCodeObj != null && !customCodeObj.toString().isBlank()) {
        marketIdentifiers.put("custom_product_code", customCodeObj.toString().trim());
      }
      // =========================================================================

      return MarketExtractedData.builder()
          .isMasterData(true)
          .marketIdentifiers(marketIdentifiers)
          .name(nameObj != null ? nameObj.toString() : "")
          .originalName(engNameObj != null ? engNameObj.toString() : "")
          .salePrice(salePrice)
          .stock(stock)
          .detailHtml(detailHtml)
          .images(images)
          .rawData(productNode)
          .build();

    } catch (Exception e) {
      // 에러의 원인을 정확히 추적하기 위해 e 전체를 로그로 찍습니다.
      log.error("카페24 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage(), e);
      throw new RuntimeException("카페24 상품 파싱 중 오류 발생", e);
    }
  }


  // 🚀 카페24 전용 '상품 메모 API' 호출 로직
  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 하위 리소스인 /memos 엔드포인트를 호출합니다.
    String path = "/admin/products/" + marketProductNo + "/memos";
    Map<String, Object> requestObj = new java.util.HashMap<>();
    requestObj.put("author_id", properties.getMallId());
    requestObj.put("memo", syncMessage);

    Map<String, Object> jsonBody = new java.util.HashMap<>();
    jsonBody.put("request", requestObj);

    try {
      // 응답받은 Raw JSON 문자열
      String responseStr = webClient.requestWithBody("POST", path, jsonBody);
      // 🚀 ObjectMapper를 활용해 유니코드를 한글로 풀고, 예쁜 JSON 형태로 포매팅
      String readableResponse = objectMapper.readTree(responseStr).toPrettyString();

      log.info("카페24 상품({})에 매칭 메모 등록 성공", marketProductNo);
      // log.info("카페24 상품({})에 매칭 메모 등록 성공: \n{}", marketProductNo, readableResponse);

    } catch (Exception e) {
      log.error("카페24 상품 메모 등록 실패: {}", e.getMessage());
    }
  }
}