package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductWriter {

  private final ProductRepository productRepository;

  // 단건 생성 (기존 Appender 역할)
  public Product write(Product product) {
    // [사전 검증 예시] 저장하기 전에 뭔가 꼭 확인해야 한다면 여기서!
    if (product.getSku() == null) {
      throw new IllegalArgumentException("SKU가 없는 상품은 저장할 수 없습니다.");
    }
    return productRepository.save(product);
  }
}