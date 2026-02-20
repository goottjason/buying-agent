package com.sbshop.agent.core.domain.product.vo;

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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Builder가 내부적으로 쓰도록 열어두되, 외부에서는 못 쓰도록 숨김
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogisticsInfo {
  @Column(name = "stock", nullable = false)
  private Integer stock;

  @Column(name = "weight", nullable = true, precision = 10, scale = 2)
  private BigDecimal weight;

  @Column(name = "bundle_quantity", nullable = true)
  private Integer bundleQuantity;
}