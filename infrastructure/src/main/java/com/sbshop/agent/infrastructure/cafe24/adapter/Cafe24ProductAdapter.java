package com.sbshop.agent.infrastructure.cafe24.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.product.port.Cafe24ProductDto;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.infrastructure.cafe24.client.Cafe24WebClient;
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
  private final ObjectMapper objectMapper; // JSON 파싱용

  @Override
  public Optional<String> findProductNoBySku(String sku) {
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
  public Cafe24ProductDto getProductDetails(String marketProductNo) {
    // 1. GET 요청 쏘기
    String responseJson = webClient.get("/admin/products/" + marketProductNo);

    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode productNode = root.path("product"); // 카페24는 응답을 "product"로 감쌉니다.

      // 2. HTML과 이미지 추출
      String detailHtml = productNode.path("detail_image").asText(""); // HTML이 보통 여기에 담깁니다.

      List<String> images = new ArrayList<>();
      if (productNode.has("list_image")) images.add(productNode.path("list_image").asText());
      if (productNode.has("tiny_image")) images.add(productNode.path("tiny_image").asText());
      // (필요에 따라 추가 이미지 파싱 로직 구현)

      return Cafe24ProductDto.builder()
          .detailHtml(detailHtml)
          .images(images)
          .build();

    } catch (Exception e) {
      log.error("카페24 상품 정보 파싱 실패: {}", e.getMessage());
      throw new RuntimeException("상품 정보 파싱 중 오류 발생");
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