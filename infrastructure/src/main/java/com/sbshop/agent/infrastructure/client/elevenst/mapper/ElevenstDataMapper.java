package com.sbshop.agent.infrastructure.client.elevenst.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class ElevenstDataMapper {

  /**
   * [식별자 조립] 11번가의 고유 ID들을 모읍니다.
   */
  public Map<String, String> buildIdentifiers(String prdNo, JsonNode rootNode) {
    Map<String, String> ids = new HashMap<>();

    // 1. 마스터 식별자 (11번가 상품번호)
    if (prdNo != null && !prdNo.isBlank()) {
      ids.put("prdNo", prdNo);
    }

    // 2. 판매자 자체 상품 코드 (우리의 자체 SKU)
    // 💡 주의: XmlMapper는 XML 태그의 대소문자를 그대로 유지합니다.
    // 실제 11번가 API 응답의 대소문자(예: sellerPrdCd 인지 SellerPrdCd 인지) 확인이 필요합니다!
    String sellerPrdCd = rootNode.path("sellerPrdCd").asText("");
    if (!sellerPrdCd.isBlank()) {
      ids.put("sellerPrdCd", sellerPrdCd);
    }

    return ids;
  }

  public BigDecimal getPrice(JsonNode rootNode) {
    // 11번가 판매가 (SelPrc)
    String priceStr = rootNode.path("SelPrc").asText("");

    // 빈 값 방어 로직
    if (priceStr.isBlank()) {
      return BigDecimal.ZERO;
    }

    // 숫자 외의 콤마(,) 등이 섞여 들어올 경우를 대비한 안전한 파싱
    return new BigDecimal(priceStr.replaceAll("[^0-9]", ""));
  }

  public int getStock(JsonNode rootNode) {
    // 11번가 재고 (StckQty)
    String stockStr = rootNode.path("StckQty").asText("");

    if (stockStr.isBlank()) {
      return 0;
    }
    return Integer.parseInt(stockStr.replaceAll("[^0-9]", ""));
  }
}