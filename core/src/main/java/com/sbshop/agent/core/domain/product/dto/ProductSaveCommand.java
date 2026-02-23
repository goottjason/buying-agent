package com.sbshop.agent.core.domain.product.dto;

import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.model.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.model.vo.SourcingInfo;
import lombok.Getter;
import lombok.Setter;

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