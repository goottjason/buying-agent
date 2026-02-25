package com.sbshop.agent.core.domain.product.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Builder가 내부적으로 쓰도록 열어두되, 외부에서는 못 쓰도록 숨김
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PriceInfo {
  @Column(name = "cost_price", nullable = true, precision = 15, scale = 2)
  private BigDecimal costPrice;

  @Column(name = "exchange_rate", nullable = true, precision = 10, scale = 2)
  private BigDecimal exchangeRate;

  @Column(name = "delivery_fee", nullable = true, precision = 15, scale = 2)
  private BigDecimal deliveryFee;

  @Column(name = "margin_rate", nullable = true, precision = 5, scale = 2)
  private BigDecimal marginRate;

  @Column(name = "sale_price", nullable = false, precision = 15, scale = 0)
  private BigDecimal salePrice;
}