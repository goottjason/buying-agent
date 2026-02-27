package com.sbshop.agent.infrastructure.external.elevenst.mapper;

import com.sbshop.agent.infrastructure.external.elevenst.parser.ElevenstProductParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ElevenstDataMapper {

  private final ElevenstProductParser parser;

  /**
   * [식별자 조립] 11번가의 고유 ID들을 모읍니다.
   */
  public Map<String, String> buildIdentifiers(String prdNo, Document doc) {
    Map<String, String> ids = new HashMap<>();

    // 1. 마스터 식별자 (11번가 상품번호)
    ids.put("prdNo", prdNo);

    // 2. 판매자 자체 상품 코드 (우리의 자체 SKU)
    ids.put("sellerPrdCd", parser.getText(doc, "SellerPrdCd"));

    return ids;
  }

  public BigDecimal getPrice(Document doc) {
    // 11번가 판매가 태그 (API 문서 기준 SelPrc 등)
    String priceStr = parser.getText(doc, "SelPrc");
    return new BigDecimal(priceStr.isBlank() ? "0" : priceStr);
  }

  public int getStock(Document doc) {
    // 11번가 재고 태그
    String stockStr = parser.getText(doc, "StckQty");
    return stockStr.isBlank() ? 0 : Integer.parseInt(stockStr);
  }
}