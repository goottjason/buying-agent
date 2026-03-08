package com.sbshop.agent.core.domain.market.repository;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import java.util.Optional;

public interface MarketRegistrationRepository {

  List<MarketRegistration> findByProductIdIn(List<Long> productIds);
  List<MarketRegistration> findByProductId(Long productId);
  Optional<MarketRegistration> findByProductIdAndMarketType(Long productId, MarketType marketType);


  MarketRegistration save(MarketRegistration marketRegistration);


  Optional<Product> findProductByCafe24ProductCode(String cafe24ProductCode);

  List<MarketRegistration> findAllByMarketType(MarketType marketType);

  void deleteByProductId(Long productId);

  List<MarketRegistration> findAllByMarketTypeWithProduct(MarketType marketType);

}