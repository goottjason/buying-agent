package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarketRegistrationFinder {
  private final MarketRegistrationRepository repository;

  // 상품 엔티티와 마켓 타입으로 기존 연동 기록이 있는지 찾습니다.
  public Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType) {
    return repository.findByProductIdAndMarketType(productId, marketType);
  }
}