package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketAdapterRouter;
import com.sbshop.agent.core.domain.product.port.MarketClient;
// 🚀 [수정] 개발자님이 이미 만들어두신 라우터 사용!
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
  // 🚀 [핵심 수정] 쓸데없는 Factory 대신, 기존에 잘 만들어두신 Router를 재사용합니다!
  private final MarketAdapterRouter adapterRouter;

  @Async // 🚀 백그라운드 스레드에서 실행되도록! (Application 클래스에 @EnableAsync 필수)
  public void asyncUpdateMarketImages(Long productId) {
    log.info("🚀 [비동기] 마켓 동기화 프로세스 시작 (ProductID: {})", productId);

    Product product = productFinder.findById(productId).orElseThrow();
    List<MarketRegistration> registrations = registrationFinder.findAllByProductId(productId);

    for (MarketRegistration reg : registrations) {
      try {
        // 해당 마켓의 어댑터 찾기
        MarketClient adapter = adapterRouter.getAdapter(reg.getMarketType());

        // 🚀 [수정] getMarketItemIds() (복수형) -> 보통 DB 설계상 getMarketItemId() (단수형) 일 확률이 높습니다.
        // 그리고 이 메서드(updateProductImageAndHtml)는 아래 2번 스텝에서 Port 인터페이스에 추가할 겁니다!
        adapter.updateProductImageAndHtml(reg.getMarketIdentifiers(), product);

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