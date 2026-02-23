package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketRegistrationRecorder {

  private final MarketRegistrationFinder finder;
  private final MarketRegistrationAppender appender;

  /**
   * 마켓 연동 기록이 있으면 가져오고, 없으면 새로 만들어서 동기화 완료(Synced) 처리합니다.
   */
  public void recordSyncSuccess(Product product, MarketType marketType, String marketProductNo) {
    MarketRegistration registration = finder.findByProductIdAndMarketType(product.getId(), marketType)
        .orElseGet(() -> appender.append(
            MarketRegistration.builder()
                .product(product)
                .marketType(marketType)
                .marketProductName(product.getName())
                .marketIdentifiers(Map.of("product_no", marketProductNo))
                .build()
        ));

    registration.markAsSynced();
  }
}