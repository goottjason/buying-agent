package com.sbshop.agent.infrastructure.product;

import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

  private final ProductJpaRepository productJpaRepository;

  @Override
  public Product save(Product product) {
    return productJpaRepository.save(product);
  }

  @Override
  public Optional<Product> findById(Long id) {
    return productJpaRepository.findById(id);
  }

  @Override
  public Optional<Product> findBySku(String sku) {
    return productJpaRepository.findBySku(sku);
  }
}
