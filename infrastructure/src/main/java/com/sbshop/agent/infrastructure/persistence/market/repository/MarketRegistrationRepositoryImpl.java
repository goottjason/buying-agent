package com.sbshop.agent.infrastructure.persistence.market.repository;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MarketRegistrationRepositoryImpl implements MarketRegistrationRepository {

  private final MarketRegistrationJpaRepository jpaRepository;

  @Override
  public MarketRegistration save(MarketRegistration marketRegistration) {
    return jpaRepository.save(marketRegistration);
  }

  @Override
  public List<MarketRegistration> findByProductId(Long productId) {
    return jpaRepository.findByProductId(productId);
  }

  @Override
  public Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType) {
    return jpaRepository.findByProductIdAndMarketType(productId, marketType);
  }

  @Override
  public Optional<Product> findProductByCafe24ProductCode(String cafe24ProductCode) {
    return jpaRepository.findProductByCafe24ProductCode(cafe24ProductCode);
  }

  @Override
  public List<MarketRegistration> findAllByMarketType(MarketType marketType) {
    return jpaRepository.findAllByMarketType(marketType);
  }

  @Override
  public void deleteByProductId(Long productId) {
    jpaRepository.deleteByProductId(productId);
  }

  @Override
  public List<MarketRegistration> findAllByMarketTypeWithProduct(MarketType marketType) {
    return jpaRepository.findAllByMarketTypeWithProduct(marketType);
  }

  @Override
  public List<MarketRegistration> findByProductIdIn(List<Long> productIds) {
    return jpaRepository.findByProductIdIn(productIds);
  }
}