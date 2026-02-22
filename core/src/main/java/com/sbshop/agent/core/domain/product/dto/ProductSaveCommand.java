package com.sbshop.agent.core.domain.product.dto;

import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.vo.SourcingInfo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductSaveCommand {
  private String sku;
  private String name;
  private String originalName;
  private String brand;
  private CategoryType category;
  private SourcingInfo sourcingInfo;
  private ProductSpec productSpec;
  private PriceInfo priceInfo;
  private LogisticsInfo logisticsInfo;
  private String searchKeywords;
  private String memo;
  private String detailHtml;
}