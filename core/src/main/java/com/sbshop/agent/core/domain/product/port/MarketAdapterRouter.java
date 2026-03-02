package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MarketAdapterRouter {

  // List 대신 빠른 O(1) 탐색을 위해 Map으로 캐싱합니다.
  private final Map<MarketType, MarketSyncPort> adapterMap;

  // 🚀 스프링이 4개의 어댑터(List)를 주입해주면, 생성자에서 즉시 Map으로 변환합니다!
  public MarketAdapterRouter(List<MarketSyncPort> adapters) {
    this.adapterMap = adapters.stream()
        .collect(Collectors.toMap(MarketSyncPort::getSupportedMarket, adapter -> adapter));
  }

  /**
   * 타겟 마켓에 맞는 어댑터를 반환합니다.
   */
  public MarketSyncPort getAdapter(MarketType marketType) {
    MarketSyncPort adapter = adapterMap.get(marketType);
    if (adapter == null) {
      throw new IllegalArgumentException("지원하지 않는 마켓입니다: " + marketType);
    }
    return adapter;
  }
}