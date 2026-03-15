package com.sbshop.agent.core.domain.product.repository;

import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

  Page<Product> findAll(Pageable pageable);

  // 상품명 또는 SKU로 검색 (대소문자 무시, LIKE %keyword%)
  // Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(String name, String sku, Pageable pageable);
  Page<Product> searchByNameOrSku(String keyword, Pageable pageable);

  List<Product> findAll();
  Product save(Product product);
  Optional<Product> findById(Long id);

  Optional<Product> findBySku(String sku);


  List<Product> findBySkuIn(List<String> skus);

  boolean existsBySku(String sku);

  List<Product> findAllByIds(List<Long> unmatchedProductIds);

  List<Product> saveAll(List<Product> products);

  String findMaxSkuByPrefix(String skuPrefix);
}