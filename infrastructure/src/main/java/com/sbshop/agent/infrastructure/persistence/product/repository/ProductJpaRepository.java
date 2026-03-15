package com.sbshop.agent.infrastructure.persistence.product.repository;

import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

  @Query("SELECT p FROM Product p " +
      "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
      "   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  Page<Product> searchByNameOrSku(@Param("keyword") String keyword, Pageable pageable);

  Optional<Product> findBySku(String sku);

  List<Product> findBySkuIn(List<String> skus);

  boolean existsBySku(String sku);

  /**
   * 🚀 특정 프리픽스(예: 20260315IHB)로 시작하는 SKU 중 가장 큰 값을 조회합니다.
   * 결과 예시: "20260315IHB005" (없으면 null 반환)
   */
  @Query("SELECT MAX(p.sku) FROM Product p WHERE p.sku LIKE :prefix%")
  String findMaxSkuByPrefix(@Param("prefix") String prefix);
}
