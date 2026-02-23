package com.sbshop.agent.infrastructure.market.repository;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketRegistrationJpaRepository extends JpaRepository<MarketRegistration, Long> {

  // Spring Data JPA의 메서드 이름 규칙을 이용해 자동으로 쿼리를 생성합니다.
  // 엔티티에 Product 객체가 연관관계로 매핑되어 있다면 Product의 id를 찾기 위해 ProductId를 사용합니다.
  Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType);

  List<MarketRegistration> findByProductId(Long productId);
}