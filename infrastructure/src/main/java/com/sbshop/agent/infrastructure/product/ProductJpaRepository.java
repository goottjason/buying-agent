package com.sbshop.agent.infrastructure.product;

import com.sbshop.agent.core.domain.product.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

  Optional<Product> findBySku(String sku);

}
