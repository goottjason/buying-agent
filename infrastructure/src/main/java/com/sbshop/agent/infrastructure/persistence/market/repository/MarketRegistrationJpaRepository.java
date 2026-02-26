package com.sbshop.agent.infrastructure.persistence.market.repository;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRegistrationJpaRepository extends JpaRepository<MarketRegistration, Long> {

  // Spring Data JPA의 메서드 이름 규칙을 이용해 자동으로 쿼리를 생성합니다.
  // 엔티티에 Product 객체가 연관관계로 매핑되어 있다면 Product의 id를 찾기 위해 ProductId를 사용합니다.
  Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType);

  List<MarketRegistration> findByProductId(Long productId);

  // JSON 타입의 컬럼(market_identifiers)에서 product_code를 빼내서 검색하는 쿼리
  @Query(value = "SELECT p.* FROM products p " +
      "JOIN market_registrations mr ON p.id = mr.product_id " +
      "WHERE mr.market_type = 'CAFE24' " +
      "AND JSON_EXTRACT(mr.market_identifiers, '$.product_code') = :cafe24Code",
      nativeQuery = true)
  Optional<Product> findProductByCafe24ProductCode(@Param("cafe24Code") String cafe24Code);

  List<MarketRegistration> findAllByMarketType(MarketType marketType);
}