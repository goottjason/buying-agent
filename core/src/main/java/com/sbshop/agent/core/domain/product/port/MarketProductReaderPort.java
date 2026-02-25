package com.sbshop.agent.core.domain.product.port;

import java.util.Optional;

public interface MarketProductReaderPort extends MarketBasePort {
  // 1단계: SKU로 마켓의 고유 상품 번호만 가볍게 찾아오기
  Optional<String> findMarketProductNoBySku(String sku);
}
