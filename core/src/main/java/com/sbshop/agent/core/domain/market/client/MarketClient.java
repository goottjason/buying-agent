package com.sbshop.agent.core.domain.market.client;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import java.util.List;
import java.util.Map;

// 조회, 메모 남기기 등 동기화 전용 계약서
public interface MarketClient {

  // 자신이 어떤 마켓을 담당하는지 반환
  MarketType getSupportedMarket();

  /**
   * 우리 로컬 상품 데이터를 해당 마켓의 규격으로 변환하여 전송하고,
   * 마켓에서 발급한 고유 상품 ID를 반환한다.
   */
  Map<String, String> publish(Product product);










  MarketItemInfo extractMarketItem(String marketItemId); // 단건 상세 파싱
  MarketItemInfo parseLocalData(Map<String, Object> rawData); // 우리 DB에 저장된 날것의 Map 데이터를 파싱해서 공통 규격으로 바꿔주는 놈

  Map<String, Object> syncPriceAndStock(
      String marketItemId,
      Map<String, Object> currentRawData, // 기존 데이터
      Integer price,
      Integer stock);
  // 🚀 [2&3단계 신규] 이미지와 HTML을 동기화하는 메서드
  Map<String, Object> syncImagesAndHtml(
      String marketItemId,
      Map<String, Object> currentRawData, // 기존 데이터
      List<String> hostedImages,          // 새롭게 Cloudflare에 올라간 이미지 URL 목록
      String newDetailHtml                // 치환이 완료된 새 HTML
  );




  /*List<String> fetchAllMarketItemIds(); // 타겟 마켓의 등록된 모든 상품 ID 추출하여 List 생성
  boolean deleteMarketProduct(String marketItemId); // 🚀 [신규] 유령 상품 삭제 API

  default void correctMarketSku(String marketItemId, String realSku) {
    // do nothing
  }

  // 🚀 [추가] 마켓별 어댑터들이 필수로 구현해야 할 "이미지/HTML 업데이트" 명령서 추가!
  void updateProductImageAndHtml(Map<String, String> identifiers, Product product);*/
}
