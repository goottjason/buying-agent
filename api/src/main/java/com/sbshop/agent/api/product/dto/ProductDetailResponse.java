package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import java.math.BigDecimal;
import lombok.Builder;

import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record ProductDetailResponse(
    Long id,
    String sku,
    String name,
    BigDecimal price,
    Integer stock,
    String memo,
    // (선택) 프론트에서 마켓별 연동 상태를 쉽게 그리기 위해 Map으로 변환해서 내려줌
    Map<String, String> marketCodes
) {
  public static ProductDetailResponse from(ProductMarketAggregate aggregate) {
    var product = aggregate.product();

    // 프론트엔드에서 {"COUPANG": "12345", "CAFE24": "P001"} 형태로 쓰기 편하게 Map 가공
    Map<String, String> marketCodes = aggregate.registrations().stream()
        .collect(Collectors.toMap(
            reg -> reg.getMarketType().name(),
            reg -> reg.getMarketItemId() != null ? reg.getMarketItemId() : ""
        ));

    return ProductDetailResponse.builder()
        .id(product.getId())
        .sku(product.getSku())
        .name(product.getName())
        .price(product.getPriceInfo().getSalePrice())
        .stock(product.getLogisticsInfo().getStock())
        .memo(product.getMemo())
        .marketCodes(marketCodes) // 🚀 프론트 친화적 조립!
        .build();
  }
}