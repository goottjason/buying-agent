package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationFinder;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.MarketPortFactory;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.component.ProductFinder; // 3000개를 긁어올 용도
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketBatchSyncProcessor {

  private final ProductFinder productFinder;
  private final ProductSyncProcessor productSyncProcessor;
  private final MarketRegistrationFinder registrationFinder; // 🚀 [추가] 연동 기록 조회기 주입!

  @Async
  public void syncAllProductsSlowly(MarketType marketType) {
    log.info("🚀 [전체 동기화 시작] {} 마켓으로의 대장정을 시작합니다...", marketType);

    // 1. 우리 DB에 있는 모든 상품을 가져옵니다.
    // (만약 상품이 수만 개라면 Pageable로 쪼개서 가져오는 것이 좋습니다만, 일단 전체 조회로 갑니다!)
    List<Product> allProducts = productFinder.findAll();
    int total = allProducts.size();

    int successCount = 0;
    int skipCount = 0;
    int failCount = 0;
    int alreadySyncedCount = 0;

    log.info("총 {}개의 상품을 순차적으로 동기화합니다. (API 제한을 위해 1초 간격으로 실행)", total);

    // 2. 루프를 돌며 하나씩 처리합니다.
    for (int i = 0; i < total; i++) {
      Product product = allProducts.get(i);
      String sku = product.getSku();

      boolean isAlreadySynced = registrationFinder.findByProductIdAndMarketType(product.getId(), marketType).isPresent();

      if (isAlreadySynced) {
        alreadySyncedCount++;
        // 로그가 3천 번 찍히면 보기 힘드니 500 단위로만 찍어줍니다.
        if (alreadySyncedCount % 500 == 0) {
          log.info("... ⏩ 기존 동기화 완료 상품 {}개 초고속 패스 중 ...", alreadySyncedCount);
        }
        continue; // 아래 로직(단건 동기화 및 1초 대기)을 모두 무시하고 즉시 다음 상품으로!
      }

      try {
        // 🚀 단건 처리기 호출! (트랜잭션은 저 안에서 알아서 걸립니다)
        productSyncProcessor.syncMarketProduct(sku, marketType);
        successCount++;
        log.info("[{}/{}] ✅ 상품 [{}] 동기화 성공", (i + 1), total, sku);

      } catch (Exception e) {
        // 🚨 에러가 나도 멈추지 않고 다음 상품으로 넘어갑니다!
        failCount++;
        log.error("[{}/{}] ❌ 상품 [{}] 동기화 실패: {}", (i + 1), total, sku, e.getMessage());
      }

      // 3. API 호출 제한(Rate Limit) 방어 - 1초(1000ms) 휴식
      try {
        Thread.sleep(1500); // 💤 여유롭게 1.5초 쉽니다. (카페24는 초당 2회 제한이 일반적입니다)
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        log.warn("동기화 중 스레드 휴식 방해받음");
        break; // 치명적인 스레드 인터럽트 시 루프 탈출
      }
    }

    log.info("🏁 [전체 동기화 완료] {} 마켓 대장정 종료! (성공: {}, 실패: {})", marketType, successCount, failCount);
  }
}