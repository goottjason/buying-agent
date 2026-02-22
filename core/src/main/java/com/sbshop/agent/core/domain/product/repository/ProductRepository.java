package com.sbshop.agent.core.domain.product.repository;

import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.dto.ProductSearchCondition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {
  Product save(Product product);
  Optional<Product> findById(Long id);

  Optional<Product> findBySku(String sku);

  Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable);

  List<Product> findBySkuIn(List<String> skus);

  boolean existsBySku(String sku);
}