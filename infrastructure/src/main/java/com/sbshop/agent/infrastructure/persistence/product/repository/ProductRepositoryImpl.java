package com.sbshop.agent.infrastructure.persistence.product.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.dto.ProductSearchCondition;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import static com.sbshop.agent.core.domain.product.model.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

  private final ProductJpaRepository productJpaRepository;
  private final JPAQueryFactory queryFactory;

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

  @Override
  public Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable) {
    // 1. 데이터 조회 쿼리
    List<Product> content = queryFactory
        .selectFrom(product)
        .where(
            keywordContains(condition.getKeyword()),
            vendorEq(condition.getVendor()),
            categoryEq(condition.getCategory())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(product.id.desc()) // 최신순 정렬
        .fetch();

    // 2. 총 개수 카운트 쿼리 (페이징을 위해 필수)
    JPAQuery<Long> countQuery = queryFactory
        .select(product.count())
        .from(product)
        .where(
            keywordContains(condition.getKeyword()),
            vendorEq(condition.getVendor()),
            categoryEq(condition.getCategory())
        );

    // 3. Page 객체로 말아서 반환
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  @Override
  public List<Product> findBySkuIn(List<String> skus) {
    return productJpaRepository.findBySkuIn(skus);
  }

  @Override
  public boolean existsBySku(String sku) {
    return productJpaRepository.existsBySku(sku);
  }

  @Override
  public List<Product> findAllByIds(List<Long> unmatchedProductIds) {
    return productJpaRepository.findAllById(unmatchedProductIds);
  }

  @Override
  public List<Product> findAll() {
    return productJpaRepository.findAll();
  }

  // --- 동적 조건(BooleanExpression) 메서드들 ---

  private BooleanExpression keywordContains(String keyword) {
    if (!StringUtils.hasText(keyword)) return null;
    // 키워드가 상품명, 원본이름, 브랜드, 메모 중 하나라도 포함되면 검색되도록 묶어줍니다.
    return product.name.containsIgnoreCase(keyword)
        .or(product.originalName.containsIgnoreCase(keyword))
        .or(product.brand.containsIgnoreCase(keyword))
        .or(product.memo.containsIgnoreCase(keyword));
  }

  private BooleanExpression vendorEq(VendorType vendor) {
    return vendor != null ? product.sourcingInfo.vendor.eq(vendor) : null;
  }

  private BooleanExpression categoryEq(CategoryType category) {
    return category != null ? product.category.eq(category) : null;
  }
}
