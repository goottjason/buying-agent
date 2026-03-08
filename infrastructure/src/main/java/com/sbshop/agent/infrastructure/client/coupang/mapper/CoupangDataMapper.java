package com.sbshop.agent.infrastructure.client.coupang.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CoupangDataMapper {

  private final ObjectMapper objectMapper;
  /**
   * [식별자 조립] 쿠팡의 필수 식별자 세트를 모읍니다.
   */
  public Map<String, String> buildIdentifiers(String sellerProductId, JsonNode firstItem) {

    Map<String, String> ids = new HashMap<>();

    // 마스터 식별자 (API 통신용 키)
    ids.put("sellerProductId", sellerProductId);

    // 옵션 식별자 및 바코드 (재고/가격 수정 시 필수)
    ids.put("vendorItemId", firstItem.path("vendorItemId").asText(""));
    ids.put("barcode", firstItem.path("barcode").asText(""));

    // 판매자 자체 상품 코드 (여기에 카페24 P000... 코드가 들어있을 수 있습니다!)
    ids.put("externalVendorSku", firstItem.path("externalVendorSku").asText(""));

    return ids;
  }

  // 🚀 어댑터에 있던 보기 싫은 TypeReference 코드를 매퍼 안으로 숨깁니다!
  public Map<String, Object> buildRawData(JsonNode dataNode) {
    return objectMapper.convertValue(
        dataNode,
        new TypeReference<Map<String, Object>>() {}
    );
  }

  /**
   * 🚀 [쿠팡 전용 마법] 조각난 contents 배열을 통짜 HTML로 조립합니다!
   */
  public String extractMergedHtmlDescription(JsonNode firstItem) {
    JsonNode contents = firstItem.path("contents");
    if (!contents.isArray()) return "";

    StringBuilder htmlBuilder = new StringBuilder();

    for (JsonNode content : contents) {
      String detailType = content.path("detailType").asText("");
      String contentValue = content.path("content").asText("");

      if ("TEXT".equalsIgnoreCase(detailType) || "HTML".equalsIgnoreCase(detailType)) {
        htmlBuilder.append(contentValue).append("<br/>");
      } else if ("IMAGE".equalsIgnoreCase(detailType)) {
        // 💡 이미지 URL이면 강제로 <img> 태그로 변환해서 합칩니다!
        // 이렇게 해야 우리의 HtmlImageExtractor가 냄새를 맡고 파싱할 수 있습니다.
        htmlBuilder.append("<img src='").append(contentValue).append("'/><br/>");
      }
    }
    return htmlBuilder.toString();
  }

  public BigDecimal getPrice(JsonNode firstItem) {
    String priceStr = firstItem.path("salePrice").asText("0");
    return new BigDecimal(priceStr.isBlank() ? "0" : priceStr);
  }

  public int getStock(JsonNode firstItem) {
    // 쿠팡은 재고 API가 별도로 있지만, 기본 정보의 최대 구매 수량 등을 임시로 활용할 수 있습니다.
    return firstItem.path("maximumBuyCount").asInt(0);
  }
}