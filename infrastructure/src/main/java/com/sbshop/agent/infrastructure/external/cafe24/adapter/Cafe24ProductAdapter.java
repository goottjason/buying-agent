package com.sbshop.agent.infrastructure.external.cafe24.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.cafe24.client.Cafe24WebClient;
import java.util.Optional;
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
    String responseJson = webClient.get("/admin/products/" + marketProductNo);

    try {
      // 1. JSON 문자열을 Map으로 파싱
      Map<String, Object> responseMap = objectMapper.readValue(
          responseJson,
          new TypeReference<Map<String, Object>>() {}
      );

      // 2. 카페24 API 응답은 보통 {"product": { 실제 데이터... }} 형태이므로 알맹이만 꺼냅니다.
      @SuppressWarnings("unchecked")
      Map<String, Object> productNode = (Map<String, Object>) responseMap.get("product");

      // 3. (임시) html과 이미지는 나중에 매핑하기로 하고, 일단 rawData만 꽉 채워서 보냅니다.
      return MarketExtractedData.builder()
          .detailHtml("")
          .images(new ArrayList<>())
          .rawData(productNode)
          .build();

    } catch (Exception e) {
      log.error("카페24 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("카페24 상품 파싱 중 오류 발생");
    }
  }



  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 카페24의 internal_product_name(자체 상품 코드) 또는 custom_product_code를 메모용으로 사용합니다.
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put("internal_product_name", syncMessage);

    Map<String, Object> jsonBody = new HashMap<>();
    jsonBody.put("product_no", Integer.valueOf(marketProductNo));
    jsonBody.put("request", requestObj);

    try {
      // 내부적으로 JSON 문자열로 변환하여 PUT 통신
      webClient.put("/admin/products/" + marketProductNo, jsonBody);
      log.info("카페24 상품({})에 동기화 메모 남기기 성공", marketProductNo);
    } catch (Exception e) {
      log.error("카페24 메모 업데이트 실패: {}", e.getMessage());
    }
  }
}