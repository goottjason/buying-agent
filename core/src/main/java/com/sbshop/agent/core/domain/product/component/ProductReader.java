package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductReader {

  private final ProductRepository repository;

  public Page<Product> readProducts(Pageable pageable) {
    // TODO: 추후에 ProductSearchCondition (검색조건)이 추가되면 QueryDSL 메서드를 호출하게 됩니다.
    return repository.findAll(pageable);
  }

  public Product read(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
  }

  /*public List<Product> readAll() {
    return repository.findAll();
  }
  public Product read(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
  }

  public List<Product> readAllByIds(List<Long> ids) {
    return repository.findAllByIds(ids);
  }

  // 공통 예외 처리 로직을 품은 단건 조회
  public Product readBySku(String sku) {
    return repository.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));
  }

  // 일괄 작업을 위한 다건 조회
  public List<Product> readAllBySkus(List<String> skus) {
    List<Product> products = repository.findBySkuIn(skus);
    if (products.isEmpty()) {
      throw new IllegalArgumentException("요청한 SKU에 해당하는 상품이 하나도 없습니다.");
    }
    return products;
  }*/
}