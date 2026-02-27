package com.sbshop.agent.core.domain.market.repository;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import java.util.Optional;

public interface MarketRegistrationRepository {
  MarketRegistration save(MarketRegistration marketRegistration);
  List<MarketRegistration> findByProductId(Long productId); // 상품 ID로 연동정보 찾기

  Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType);

  Optional<Product> findProductByCafe24ProductCode(String cafe24ProductCode);

  List<MarketRegistration> findAllByMarketType(MarketType marketType);

  void deleteByProductId(String marketProductId);
}