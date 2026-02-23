package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.domain.product.dto.ProductBulkUpdateCommand;
import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ProductBulkUpdateRequest {

  @NotEmpty(message = "수정할 상품의 SKU 목록은 필수입니다.")
  @Schema(description = "수정할 상품 SKU 목록")
  private List<String> skus;

  @Schema(description = "일괄 변경할 카테고리 (null이면 변경 안 함)")
  private CategoryType category;

  @Schema(description = "일괄 추가/변경할 검색어")
  private String searchKeywords;

  @Schema(description = "일괄 변경할 메모")
  private String memo;

  // API Request -> Core Command 변환
  public ProductBulkUpdateCommand toCommand() {
    ProductBulkUpdateCommand command = new ProductBulkUpdateCommand();
    command.setSkus(this.skus);
    command.setCategory(this.category);
    command.setSearchKeywords(this.searchKeywords);
    command.setMemo(this.memo);
    return command;
  }
}