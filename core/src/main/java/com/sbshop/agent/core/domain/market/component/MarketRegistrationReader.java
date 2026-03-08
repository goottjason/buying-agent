package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarketRegistrationReader {
  private final MarketRegistrationRepository repository;

  // 상품 ID 리스트(IN 조건)로 여러 마켓아이템 조회
  public List<MarketRegistration> readAllByProductIds(List<Long> productIds) {
    return repository.findByProductIdIn(productIds);
  }
  // 상품 ID로 여러 마켓아이템 조회
  public List<MarketRegistration> readAllByProductId(Long productId) {
    return repository.findByProductId(productId);
  }
  // 상품 ID와 MarketType으로 특정 마켓아이템 조회
  public MarketRegistration readByProductIdAndMarketType(Long productId, MarketType marketType) {
    return repository.findByProductIdAndMarketType(productId, marketType)
        .orElseThrow(() -> new IllegalArgumentException(marketType + " 마켓 연동 정보가 없습니다."));
  }


  public List<MarketRegistration> readAllByMarketType(MarketType marketType) {
    return repository.findAllByMarketTypeWithProduct(marketType);
  }

  public Optional<MarketRegistration> findByProductAndMarket(Long productId, MarketType marketType) {
    return repository.findByProductIdAndMarketType(productId, marketType);
  }
}

