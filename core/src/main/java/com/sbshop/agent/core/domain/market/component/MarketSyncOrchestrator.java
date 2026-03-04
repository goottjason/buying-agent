package com.sbshop.agent.core.domain.market.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSyncOrchestrator {

  private final ProductFinder productFinder;
  private final MarketRegistrationFinder registrationFinder;
  // MarketType별로 구현된 어댑터들(Smartstore, 11st, Coupang 등)을 Map이나 List로 주입받음
  private final MarketSyncPortFactory portFactory;

  @Async // 🚀 백그라운드 스레드에서 실행되도록! (Application 클래스에 @EnableAsync 필수)
  public void asyncUpdateMarketImages(Long productId) {
    log.info("🚀 [비동기] 마켓 동기화 프로세스 시작 (ProductID: {})", productId);

    Product product = productFinder.findById(productId).orElseThrow();
    List<MarketRegistration> registrations = registrationFinder.findAllByProductId(productId);

    for (MarketRegistration reg : registrations) {
      try {
        // 해당 마켓의 어댑터 찾기
        MarketSyncPort adapter = portFactory.getAdapter(reg.getMarketType());

        // 🚀 각 마켓 어댑터의 [상품 수정 API] 호출! (내일 우리가 구현할 부분)
        adapter.updateProductImageAndHtml(reg.getMarketItemIds(), product);

        log.info("   ✅ [{}] 마켓 이미지/HTML 업데이트 성공!", reg.getMarketType());
      } catch (Exception e) {
        // 비동기 작업 중 에러가 나도 다른 마켓 업데이트에 영향을 주지 않도록 Catch!
        log.error("   ❌ [{}] 마켓 업데이트 실패: {}", reg.getMarketType(), e.getMessage());
        // TODO: 실패 이력 DB 저장 또는 슬랙 알림 로직 추가
      }
    }
    log.info("🏁 [비동기] 마켓 동기화 프로세스 종료");
  }
}