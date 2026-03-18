package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductValidator {

  /**
   * 🚀 마켓 등록 전, 상품의 필수 값들이 완벽한지 검사합니다.
   */
  public void validateForPublish(Product product) {
    // 1. 카테고리 검증
    if (product.getCategory() == null) {
      throw new IllegalArgumentException("카테고리가 매핑되지 않은 상품입니다. 카테고리를 먼저 설정해주세요.");
    }

    // 2. 가격 검증
    if (product.getPriceInfo() == null ||
        product.getPriceInfo().getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("판매가가 0원 이하입니다. 판매가를 올바르게 설정해주세요.");
    }

    // 3. 수량 검증
    if (product.getLogisticsInfo() == null ||
        product.getLogisticsInfo().getBundleQuantity() <= 0) {
      throw new IllegalArgumentException("묶음 수량(단위)이 설정되지 않았습니다.");
    }

    // 4. 상세 HTML 검증
    if (product.getDetailHtml() == null || product.getDetailHtml().isBlank()) {
      throw new IllegalArgumentException("상세 설명(HTML)이 비어있습니다. 상세 HTML을 생성해주세요.");
    }

    // 5. 이미지 최소 1장 이상 검증
    if (product.getImageInfo() == null || product.getImageInfo().getHostedImages().isEmpty()) {
      throw new IllegalArgumentException("업로드된 이미지가 없습니다. 이미지를 최소 1장 이상 등록해주세요.");
    }
  }
}