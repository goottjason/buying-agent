package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import java.math.BigDecimal;
import lombok.Builder;
import java.util.List;

/**
 * 프론트엔드 AG Grid에 한 줄(Row)로 뿌려질 평탄화된 데이터 포맷
 */
@Builder
public record ProductGridResponse(
    Long id,
    String sku,
    String name,
    BigDecimal price,
    Integer stock,
    // 마켓별 연동 코드 (비어있으면 null 또는 빈 문자열)
    String coupangCode,
    String cafe24Code,
    String smartstoreCode,
    String elevenstCode
) {

  // 🚀 API 모듈이 주도적으로 엔티티들을 뜯어서 화면 모양(Grid)으로 조립합니다.
  public static ProductGridResponse from(ProductMarketAggregate aggregate) {
    Product product = aggregate.product();
    String coupang = null, cafe24 = null, smartstore = null, elevenst = null;

    for (MarketRegistration reg : aggregate.registrations()) {
      String marketItemId = reg.getMarketItemId();
      switch (reg.getMarketType()) {
        case COUPANG -> coupang = marketItemId;
        case CAFE24 -> cafe24 = marketItemId;
        case SMARTSTORE -> smartstore = marketItemId;
        case ELEVENST -> elevenst = marketItemId;
      }
    }

    return ProductGridResponse.builder()
        .id(product.getId())
        .sku(product.getSku())
        .name(product.getName())
        .price(product.getPriceInfo().getSalePrice())
        .stock(product.getLogisticsInfo().getStock())
        .coupangCode(coupang)
        .cafe24Code(cafe24)
        .smartstoreCode(smartstore)
        .elevenstCode(elevenst)
        .build();
  }
}