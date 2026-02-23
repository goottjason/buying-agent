package com.sbshop.agent.infrastructure.product.repository;

import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

  Optional<Product> findBySku(String sku);

  List<Product> findBySkuIn(List<String> skus);

  boolean existsBySku(String sku);
}
