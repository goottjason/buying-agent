package com.sbshop.agent.core.domain.product;

import java.util.List;

public interface MarketRegistrationRepository {
  MarketRegistration save(MarketRegistration marketRegistration);
  List<MarketRegistration> findByProductId(Long productId); // 상품 ID로 연동정보 찾기
}