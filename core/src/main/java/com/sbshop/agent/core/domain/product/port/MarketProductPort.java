package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.dto.MarketProductDto;
import java.util.Optional;

public interface MarketProductPort {
  MarketType getSupportedMarket();
  Optional<String> findProductNoBySku(String sku);

  MarketProductDto getProductDetails(String marketProductNo);

  void updateSyncMemo(String marketProductNo, String syncMessage);
}