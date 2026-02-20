package com.sbshop.agent.core.domain.product;

import java.util.Optional;

public interface ProductRepository {
  Product save(Product product);
  Optional<Product> findById(Long id);

  Optional<Product> findBySku(String sku);
}