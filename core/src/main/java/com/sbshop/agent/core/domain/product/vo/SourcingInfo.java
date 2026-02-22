package com.sbshop.agent.core.domain.product.vo;

import com.sbshop.agent.core.domain.product.enums.VendorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SourcingInfo {

  @Enumerated(EnumType.STRING)
  @Column(name = "vendor", nullable = false, length = 50, columnDefinition = "varchar(50)")
  private VendorType vendor;

  @Column(name = "source_url", length = 1000)
  private String sourceUrl;

  @Column(name = "manufacturer", length = 100)
  private String manufacturer;

  @Column(name = "origin", length = 50)
  private String origin;

  @Column(name = "hs_code", length = 50)
  private String hsCode;
}