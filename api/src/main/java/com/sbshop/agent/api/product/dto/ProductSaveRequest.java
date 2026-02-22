package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.domain.product.dto.ProductSaveCommand;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.vo.SourcingInfo;
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

  public ProductSaveCommand toCommand() {
    ProductSaveCommand command = new ProductSaveCommand();
    command.setSku(this.sku);
    command.setName(this.name);
    command.setOriginalName(this.originalName);
    command.setBrand(this.brand);
    command.setCategory(this.category);
    command.setSourcingInfo(this.sourcingInfo);
    command.setProductSpec(this.productSpec);
    command.setPriceInfo(this.priceInfo);
    command.setLogisticsInfo(this.logisticsInfo);
    command.setSearchKeywords(this.searchKeywords);
    command.setMemo(this.memo);
    command.setDetailHtml(this.detailHtml);
    return command;
  }
}