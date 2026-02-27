package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import java.util.List;

// 조회, 메모 남기기 등 동기화 전용 계약서
public interface MarketSyncPort {
  MarketType getSupportedMarket(); // 어떤 마켓인지?
  List<String> fetchAllMarketProductIds(); // 타겟 마켓의 등록된 모든 상품 ID 추출하여 List 생성
  MarketExtractedData extractProductData(String marketProductId); // 단건 상세 파싱
  boolean deleteMarketProduct(String marketProductId); // 🚀 [신규] 유령 상품 삭제 API

  /**
   * 🚀 [신규 추가] 마켓의 잘못된 식별자(SKU 등)를 진짜 SKU로 교정합니다.
   * 기본 구현은 '아무것도 하지 않음' 이므로,
   * 카페24, 스마트스토어, 11번가 어댑터는 이 코드를 구현할 필요가 없습니다!
   */
  default void correctMarketSku(String marketProductId, String realSku) {
    // do nothing
  }
}
