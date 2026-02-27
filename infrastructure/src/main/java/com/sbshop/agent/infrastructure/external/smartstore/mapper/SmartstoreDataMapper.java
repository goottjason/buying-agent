package com.sbshop.agent.infrastructure.external.smartstore.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.external.smartstore.parser.SmartstoreProductParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SmartstoreDataMapper {

  private final SmartstoreProductParser parser;

  /**
   * [식별자 조립] 스마트스토어의 고유 ID들을 모읍니다.
   */
  public Map<String, String> buildIdentifiers(String originProductNo, JsonNode rootNode) {
    Map<String, String> ids = new HashMap<>();

    // 1. 마스터 식별자 (API 통신용 키)
    ids.put("originProductNo", originProductNo);

    // 2. 채널 상품 번호 (스마트스토어 프론트 전시용 번호)
    JsonNode channelProducts = rootNode.path("smartstoreChannelProduct");
    if (!channelProducts.isMissingNode()) {
      ids.put("channelProductNo", channelProducts.path("channelProductNo").asText(""));
    }

    // 3. 판매자 관리 코드 (우리의 자체 SKU)
    ids.put("sellerCustomCode1", parser.getTextFromOrigin(rootNode, "sellerCustomCode1"));

    return ids;
  }

  public BigDecimal getSalePrice(JsonNode rootNode) {
    String priceStr = parser.getTextFromOrigin(rootNode, "salePrice");
    return new BigDecimal(priceStr.isBlank() ? "0" : priceStr);
  }

  public int getStockQuantity(JsonNode rootNode) {
    return getOriginProductNode(rootNode).path("stockQuantity").asInt(0);
  }

  // 헬퍼: 코드가 길어지는 것을 방지
  private JsonNode getOriginProductNode(JsonNode rootNode) {
    return parser.getOriginProductNode(rootNode);
  }
}