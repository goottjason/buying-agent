package com.sbshop.agent.core.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductFinder {

  private final ProductRepository productRepository;

  public Optional<Product> findBySku(String sku) {
    return productRepository.findBySku(sku);
  }

  // NOTE: 만약 없으면 예외를 던지는 getBySku() 같은 편의 메서드도 여기서 만들면 좋습니다.
}