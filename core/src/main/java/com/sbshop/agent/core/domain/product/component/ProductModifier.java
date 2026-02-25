package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.dto.ProductBulkUpdateCommand;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductModifier {
  private final ProductFinder productFinder; // 단건 조회를 위해 Finder 주입 (★ 상단에 추가)
  private final ProductRepository productRepository;

  // ★ 일괄 수정
  @Transactional
  public int bulkUpdate(ProductBulkUpdateCommand command) {
    // 1. 대상 상품들을 한 번에 조회합니다.
    List<Product> products = productRepository.findBySkuIn(command.getSkus());

    // 2. 🚀 공통으로 적용할 '단일 업데이트 커맨드'를 미리 하나 만들어둡니다.
    ProductUpdateCommand updateCommand = ProductUpdateCommand.builder()
        .category(command.getCategory())
        .searchKeywords(command.getSearchKeywords())
        .memo(command.getMemo())
        .build();

    // 3. 각각의 상품에게 동일한 커맨드를 던져서 스스로 업데이트하게 합니다.
    for (Product product : products) {
      product.update(updateCommand);
    }

    return products.size(); // 성공한 건수 반환
  }

  // ★ 일괄 삭제 (소프트 삭제)
  @Transactional
  public int bulkDelete(List<String> skus) {
    List<Product> products = productRepository.findBySkuIn(skus);

    for (Product product : products) {
      product.delete();
    }

    return products.size();
  }
}