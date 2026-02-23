package com.sbshop.agent.core.domain.product.port;

import java.util.Optional;

public interface MarketProductPort {

  // [추가] 0. 우리 SKU(자체상품코드)로 카페24 상품 번호를 검색합니다.
  Optional<String> findProductNoBySku(String sku);

  // 1. 카페24 상품 번호로 상품의 상세 정보를 가져옵니다.
  Cafe24ProductDto getProductDetails(String marketProductNo);

  // 2. 카페24 상품에 "우리가 관리함 + 시간"을 메모로 남깁니다.
  void updateSyncMemo(String marketProductNo, String syncMessage);
}