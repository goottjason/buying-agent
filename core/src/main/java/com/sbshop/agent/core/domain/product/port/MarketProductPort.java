package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import java.util.Optional;

public interface MarketProductPort {
  MarketType getSupportedMarket();
  Optional<String> findMarketProductNoBySku(String sku);

  MarketExtractedData getProductDetailsByMarketProductNo(String marketProductNo);

  /*"이 메서드는 내가 기본 알맹이(default)를 짜줄 테니까,
  맘에 안 드는 애들(예: 카페24)만 각자 알아서 덮어쓰기(@Override) 하고,
  나머지(쿠팡, 스마트스토어)는 그냥 내가 짜준 기본 로직 그대로 써!"*/
  // 기본(default) 구현: 번호를 먼저 찾고(find), 그 번호로 상세조회(get)하는 2단계 방식
  default Optional<MarketExtractedData> getProductDetailsBySku(String sku) {
    return findMarketProductNoBySku(sku)
        .map(this::getProductDetailsByMarketProductNo);
  }
  void updateSyncMemo(String marketProductNo, String syncMessage);
}