package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.model.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.model.vo.SourcingInfo;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductSaveRequest {
  private String sku;
  private String name;
  private String originalName;
  private String brand;
  private CategoryType category;

  // VO 통째로 받기
  private SourcingInfo sourcingInfo;
  private ProductSpec productSpec;
  private PriceInfo priceInfo;
  private LogisticsInfo logisticsInfo;

  private String searchKeywords;
  private String memo;
  private String detailHtml;


}