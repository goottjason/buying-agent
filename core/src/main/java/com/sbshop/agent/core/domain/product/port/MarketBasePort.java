package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;

public interface MarketBasePort {
  // 모든 마켓 어댑터는 자기가 어떤 마켓인지 대답할 수 있어야 합니다.
  MarketType getSupportedMarket();
}
