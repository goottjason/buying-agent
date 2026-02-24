package com.sbshop.agent.api.product.processor;

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
  private final MarketPortFactory portFactory; // 팩토리 주입

  @Async
  // [수정] 범용적인 이름으로 변경하고 MarketType을 받습니다.
  public void syncAllAsync(MarketType marketType) {
    log.info("🚀 [백그라운드 작업] {} 전체 상품 동기화를 시작합니다.", marketType);

    MarketProductPort port = portFactory.getPort(marketType); // 해당 마켓 어댑터 준비
    List<Product> allProducts = productFinder.findAll();
    int successCount = 0, failCount = 0, notFoundCount = 0;

    for (Product product : allProducts) {
      try {
        Optional<String> marketProductNoOpt = port.findMarketProductNoBySku(product.getSku());

        if (marketProductNoOpt.isPresent()) {
          // 단건 프로세서에 MarketType도 같이 넘겨줍니다.
          productSyncProcessor.syncMarketProduct(product.getSku(), marketType);
          successCount++;
        } else {
          notFoundCount++;
        }
        Thread.sleep(1000); // 마켓마다 제한이 다르니 여유 있게 조절

      } catch (Exception e) {
        log.error("🚨 상품 [{}] 동기화 실패: {}", product.getSku(), e.getMessage());
        failCount++;
      }
    }
    log.info("🏁 [작업 종료] 총: {}, 성공: {}, 미등록: {}, 실패: {}", allProducts.size(), successCount, notFoundCount, failCount);
  }

  /*@Async
  public void syncAllWithCafe24Async() {
    log.info("🚀 [백그라운드 작업] 카페24 전체 상품 동기화를 시작합니다.");

    // 1. 우리 DB에 있는 모든 상품을 가져옵니다. (나중에는 페이징이나 커서 방식으로 고도화하면 더 좋습니다)
    List<Product> allProducts = productFinder.findAll();

    int successCount = 0;
    int failCount = 0;
    int notFoundCount = 0;

    // 2. 3천 개 순회 시작!
    for (Product product : allProducts) {
      try {

        // 1. 방금 만든 검색 포트를 이용해 카페24에서 상품 번호를 찾아옵니다.
        Optional<String> cafe24ProductNoOpt = marketProductPort.findProductNoBySku(product.getSku());

        if (cafe24ProductNoOpt.isPresent()) {
          // 2. 번호를 찾았다면, 해당 번호로 상세 조회 및 메모 갱신 로직을 태웁니다!
          singleSyncProcessor.syncWithCafe24(product.getSku(), cafe24ProductNoOpt.get());
          successCount++;
        } else {
          log.warn("상품 [{}]은(는) 카페24에 등록되지 않아 건너뜁니다.", product.getSku());
          notFoundCount++;
        }
        Thread.sleep(5000);

      } catch (Exception e) {
        // 특정 상품 하나가 에러나도 전체 루프가 멈추면 안 됩니다!
        log.error("🚨 상품 [{}] 동기화 실패: {}", product.getSku(), e.getMessage());
        failCount++;
      }
    }

    log.info("🏁 [작업 종료] 총: {}, 성공: {}, 미등록(건너뜀): {}, 실패: {}",
        allProducts.size(), successCount, notFoundCount, failCount);
  }*/

}