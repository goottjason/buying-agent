package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.dto.ProductSearchCondition;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductFinder {

  private final ProductRepository productRepository;

  public List<Product> findAllProducts() {
    return productRepository.findAll();
  }

  // --- 단건 조회 ---
  public Optional<Product> findBySku(String sku) {
    return productRepository.findBySku(sku);
  }

  // (보너스) 주석으로 남겨주신 편의 메서드 구현: 없으면 예외를 던짐
  public Product getBySku(String sku) {
    return findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));
  }

  // --- ★ 추가: 다건 조회 및 검색 위임 메서드 ---
  public Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable) {
    // 나중에 여기서 DTO 변환을 하거나, 추가적인 비즈니스 검증 로직을 넣을 수 있습니다.
    return productRepository.searchProducts(condition, pageable);
  }

  public List<Product> findAll() {
    return productRepository.findAll();
  }
}