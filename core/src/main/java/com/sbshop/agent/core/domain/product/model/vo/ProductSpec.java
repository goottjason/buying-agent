package com.sbshop.agent.core.domain.product.model.vo;

import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductSpec {

  @Column(name = "barcode", length = 100)
  private String barcode;

  @Column(name = "capacity", precision = 10, scale = 2)
  private BigDecimal capacity;

  @Enumerated(EnumType.STRING)
  @Column(name = "measure_unit", length = 50, columnDefinition = "varchar(50)")
  private MeasureUnit measureUnit;
}