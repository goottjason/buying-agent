package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.domain.product.dto.ProductSearchCondition;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.enums.VendorType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductSearchRequest {

  @Parameter(description = "통합 검색어 (상품명, 브랜드 등)")
  private String keyword;

  @Parameter(description = "소싱처 필터")
  private VendorType vendor;

  @Parameter(description = "카테고리 필터")
  private CategoryType category;

  @Parameter(description = "최소 가격")
  private Integer minPrice;

  @Parameter(description = "최대 가격")
  private Integer maxPrice;

  // ★ 핵심: API 계층의 데이터를 Core 계층의 Condition으로 변환해주는 메서드
  public ProductSearchCondition toCondition() {
    ProductSearchCondition condition = new ProductSearchCondition();
    condition.setKeyword(this.keyword);
    condition.setVendor(this.vendor);
    condition.setCategory(this.category);
    condition.setMinPrice(this.minPrice);
    condition.setMaxPrice(this.maxPrice);
    return condition;
  }
}