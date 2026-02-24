package com.sbshop.agent.infrastructure.external.cafe24.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.cafe24.client.Cafe24WebClient;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24ProductAdapter implements MarketProductPort {

  private final Cafe24WebClient webClient;
  private final ObjectMapper objectMapper;

  // 팩토리가 여러 어댑터 중에서 나를 찾을 수 있게 해주는 "명찰"
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.CAFE24;
  }

  @Override
  public Optional<MarketExtractedData> getProductDetailsBySku(String sku) {
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
  }

  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {

    // 자체상품코드(internal_product_name)로 검색하고, 응답 데이터 다이어트를 위해 product_no 필드만 요청합니다.
    String path = "/admin/products?internal_product_name=" + sku + "&fields=product_no";

    String responseJson = webClient.get(path);

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode productsNode = root.path("products");

      // 배열에 결과가 1개라도 있다면 첫 번째 상품의 번호를 가져옵니다.
      if (productsNode.isArray() && productsNode.size() > 0) {
        return Optional.of(productsNode.get(0).path("product_no").asText());
      }
    } catch (Exception e) {
      log.error("카페24 SKU({}) 검색 파싱 실패: {}", sku, e.getMessage());
    }

    // 검색 결과가 없거나 에러가 나면 빈 껍데기를 반환합니다.
    return Optional.empty();
  }

  @Override
  public MarketExtractedData getProductDetailsByMarketProductNo(String marketProductNo) {

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

      // SKU 정보 (이미지 필터링용)
      String sku = (String) productNode.getOrDefault("internal_product_name", "");

      // 1. 상세 HTML
      String detailHtml = (String) productNode.getOrDefault("description", "");

      // 2. HTML 안에서 SKU 이미지 추출 및 정렬 로직
      List<String> images = new java.util.ArrayList<>();
      if (!detailHtml.isEmpty() && !sku.isEmpty()) {
        // <img src="..."> 안의 URL만 추출하는 정규식
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher matcher = pattern.matcher(detailHtml);

        while (matcher.find()) {
          String imgUrl = matcher.group(1);
          if (imgUrl.contains(sku)) {
            images.add(imgUrl);
          }
        }
        // 알파벳/숫자 오름차순 정렬 (sku-1.jpg, sku-2.jpg 순서 완벽 보장)
        Collections.sort(images);
      }

      // 3. 재고 파싱 (variants 배열의 첫 번째 품목 수량)
      int stock = 0;
      if (productNode.containsKey("variants")) {
        List<Map<String, Object>> variants = (List<Map<String, Object>>) productNode.get("variants");
        if (!variants.isEmpty()) {
          // 수량이 Object로 올 수 있으므로 안전하게 파싱
          Object quantityObj = variants.get(0).get("quantity");
          stock = quantityObj != null ? Integer.parseInt(quantityObj.toString()) : 0;
        }
      }

      // 4. 가격 파싱
      Object priceObj = productNode.get("price");
      java.math.BigDecimal salePrice = (priceObj != null) ? new java.math.BigDecimal(priceObj.toString()) : java.math.BigDecimal.ZERO;

      return MarketExtractedData.builder()
          .name((String) productNode.getOrDefault("product_name", ""))
          .originalName((String) productNode.getOrDefault("eng_product_name", ""))
          .salePrice(salePrice)
          .stock(stock)
          .detailHtml(detailHtml)
          .images(images)
          .rawData(productNode) // 원본 바구니도 잊지 않고 챙깁니다!
          .build();

    } catch (Exception e) {
      log.error("카페24 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("카페24 상품 파싱 중 오류 발생");
    }
  }

  // 🚀 카페24 전용 '상품 메모 API' 호출 로직
  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 하위 리소스인 /memos 엔드포인트를 호출합니다.
    String path = "/admin/products/" + marketProductNo + "/memos";
    Map<String, Object> requestObj = new java.util.HashMap<>();
    requestObj.put("memo", syncMessage);

    Map<String, Object> jsonBody = new java.util.HashMap<>();
    jsonBody.put("request", requestObj);

    try {
      String response = webClient.requestWithBody("POST", path, jsonBody);
      log.info("카페24 상품({})에 매칭 메모 등록 성공: {}", marketProductNo, response);
    } catch (Exception e) {
      log.error("카페24 상품 메모 등록 실패: {}", e.getMessage());
    }
  }
}