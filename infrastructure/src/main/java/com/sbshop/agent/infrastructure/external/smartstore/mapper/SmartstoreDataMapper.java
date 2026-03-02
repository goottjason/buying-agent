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

    // 1. 마스터 식별자 (API 통신용 키 - 원상품번호)
    if (originProductNo != null && !originProductNo.isBlank()) {
      ids.put("originProductNo", originProductNo);
    }

    // 2. 채널 상품 번호 (스마트스토어 프론트 전시용 상품번호)
    JsonNode channelProductNode = rootNode.path("smartstoreChannelProduct");
    if (!channelProductNode.isMissingNode()) {
      String channelProductNo = channelProductNode.path("channelProductNo").asText("");
      if (!channelProductNo.isBlank()) {
        ids.put("channelProductNo", channelProductNo);
      }
    }

    // ====================================================================
    // 🚀 3. 판매자 관리 코드 (우리의 자체 SKU) - 방금 찾은 지하 3층 벙커!
    // ====================================================================
    String sellerManagementCode = rootNode.path("originProduct")
        .path("detailAttribute")
        .path("sellerCodeInfo")
        .path("sellerManagementCode").asText("");

    if (!sellerManagementCode.isBlank()) {
      // DB의 market_identifiers 에 들어갈 Key 이름입니다.
      // 네이버 공식 명칭에 맞춰서 "sellerManagementCode"로 저장하는 것을 추천합니다!
      // (만약 기존 시스템 호환을 위해 "sellerCustomCode1"을 꼭 써야 한다면 이름을 바꿔주세요)
      ids.put("sellerManagementCode", sellerManagementCode);
    }

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