package com.sbshop.agent.infrastructure.smartstore.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketProductDto;
import com.sbshop.agent.infrastructure.smartstore.client.SmartstoreWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreProductAdapter implements MarketProductPort {

  private final SmartstoreWebClient webClient;
  private final ObjectMapper objectMapper;

  // 🚀 [핵심 1] 팩토리에게 "나는 스마트스토어 담당이야!" 라고 알려줍니다.
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.SMARTSTORE; // Enum에 SMARTSTORE가 없으면 추가해주세요!
  }

  @Override
  public Optional<String> findProductNoBySku(String sku) {
    // 스마트스토어는 판매자관리코드(sellerManagementCode)로 상품을 조회할 수 있습니다.
    String path = "/v1/products/search?sellerManagementCode=" + sku;

    try {
      String responseJson = webClient.get(path);
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode contents = root.path("contents");

      if (contents.isArray() && contents.size() > 0) {
        // 스마트스토어의 고유 상품 번호 반환
        return Optional.of(contents.get(0).path("originProductNo").asText());
      }
    } catch (Exception e) {
      log.error("스마트스토어 SKU({}) 검색 실패: {}", sku, e.getMessage());
    }
    return Optional.empty();
  }

  @Override
  public MarketProductDto getProductDetails(String marketProductNo) {
    String path = "/v2/products/origin-products/" + marketProductNo;

    try {
      String responseJson = webClient.get(path);
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode originProduct = root.path("originProduct");

      // 1. 상세 HTML 추출
      String detailHtml = originProduct.path("detailContent").asText("");

      // 2. 이미지 추출 (대표 이미지 + 추가 이미지)
      List<String> images = new ArrayList<>();
      JsonNode imagesNode = originProduct.path("images");

      if (imagesNode.has("representativeImage")) {
        images.add(imagesNode.path("representativeImage").path("url").asText());
      }
      if (imagesNode.has("optionalImages") && imagesNode.path("optionalImages").isArray()) {
        for (JsonNode optImg : imagesNode.path("optionalImages")) {
          images.add(optImg.path("url").asText());
        }
      }

      return MarketProductDto.builder()
          .detailHtml(detailHtml)
          .images(images)
          .build();

    } catch (Exception e) {
      log.error("스마트스토어 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("스마트스토어 상품 파싱 중 오류 발생");
    }
  }

  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 🚀 스마트스토어는 부분 수정이 안 되므로, GET으로 다 가져와서 고친 후 PUT으로 던집니다. (레거시 방식 유지)
    String path = "/v2/products/origin-products/" + marketProductNo;

    try {
      // 1. 기존 데이터 전체 조회
      String getResponseJson = webClient.get(path);
      Map<String, Object> getResponseMap = objectMapper.readValue(getResponseJson, new TypeReference<>() {});

      @SuppressWarnings("unchecked")
      Map<String, Object> originProduct = (Map<String, Object>) getResponseMap.get("originProduct");

      // 2. 메모 덮어쓰기 (판매자 관리 코드 또는 사용자 정의 필드 활용)
      originProduct.put("sellerManagementCode", syncMessage);

      // 3. 재포장 후 PUT 요청
      Map<String, Object> body = new HashMap<>();
      body.put("originProduct", originProduct);

      webClient.put(path, body);
      log.info("스마트스토어 상품({})에 동기화 메모 남기기 성공", marketProductNo);

    } catch (Exception e) {
      log.error("스마트스토어 메모 업데이트 실패 (ID: {}): {}", marketProductNo, e.getMessage());
    }
  }
}