package com.sbshop.agent.core.domain.product.port;

public interface MarketCommandPort extends MarketBasePort {
  // 마켓에 메모 남기기 등 상태 변경
  void updateSyncMemo(String marketProductNo, String syncMessage);
}
