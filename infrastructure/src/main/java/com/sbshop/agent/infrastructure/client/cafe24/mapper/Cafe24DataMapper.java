package com.sbshop.agent.infrastructure.client.cafe24.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.client.cafe24.parser.Cafe24ProductParser;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Cafe24DataMapper {

  private final Cafe24ProductParser parser;

  public int calculateTotalStock(JsonNode productNode) {
    JsonNode variants = parser.getVariantsNode(productNode);
    int totalStock = 0;
    if (variants.isArray()) {
      for (JsonNode variant : variants) {
        // 각 변종의 수량을 합산
        totalStock += variant.path("quantity").asInt(0);
      }
    }
    return totalStock;
  }
  /*public int calculateTotalStock(JsonNode productNode) {
    JsonNode variants = parser.getVariantsNode(productNode);
    int totalStock = 0;
    if (variants.isArray()) {
      for (JsonNode variant : variants) {
        totalStock += variant.path("quantity").asInt(0);
      }
    }
    return totalStock;
  }*/

  public String getMergedDescription(JsonNode productNode) {
    String pcDesc = parser.getText(productNode, "description");
    if (!pcDesc.isBlank()) return pcDesc;

    // PC 설명이 없으면 모바일 설명을 반환
    return parser.getText(productNode, "mobile_description");
  }

  public Map<String, String> buildIdentifiers(String marketProductId, JsonNode productNode) {
    Map<String, String> ids = new HashMap<>();
    // 1. API 호출 시 사용했던 ID
    ids.put("product_no", marketProductId);
    // 2. 카페24 내부 상품 코드 (P000...)
    ids.put("product_code", parser.getText(productNode, "product_code"));
    // 3. 자체 상품 코드 (SKU)
    ids.put("custom_product_code", parser.getText(productNode, "custom_product_code"));
    return ids;
  }

  public BigDecimal getPrice(JsonNode productNode) {
    String priceStr = parser.getText(productNode, "price");
    return new BigDecimal(priceStr.isBlank() ? "0" : priceStr);
  }
}