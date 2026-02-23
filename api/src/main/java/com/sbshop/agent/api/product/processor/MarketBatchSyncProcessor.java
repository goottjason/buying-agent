package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductFinder; // 3000개를 긁어올 용도
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
  private final ProductSyncProcessor singleSyncProcessor;
  private final MarketProductPort marketProductPort; // 검색을 위해 주입받습니다.
  /**
   * @Async 어노테이션이 붙어있으므로, 호출 즉시 백그라운드 스레드에서 실행됩니다.
   */
  @Async
  public void syncAllWithCafe24Async() {
    log.info("🚀 [백그라운드 작업] 카페24 전체 상품 동기화를 시작합니다.");

    // 1. 우리 DB에 있는 모든 상품을 가져옵니다. (나중에는 페이징이나 커서 방식으로 고도화하면 더 좋습니다)
    // NOTE: productFinder에 findAll() 메서드를 하나 추가해 두셔야 합니다!
    List<Product> allProducts = productFinder.findAll();

    int successCount = 0;
    int failCount = 0;
    int notFoundCount = 0;

    // 2. 3천 개 순회 시작!
    for (Product product : allProducts) {
      try {

        // 1. 방금 만든 검색 포트를 이용해 카페24에서 상품 번호를 찾아옵니다.
        Optional<String> cafe24ProductNoOpt = marketProductPort.findProductNoBySku(product.getSku());

        // NOTE: 우리 DB의 Product에서 카페24의 상품번호를 알아내야 합니다.
        // 예를 들어, SKU가 카페24의 자체 상품 코드와 같다면 그 값을 추출하거나,
        // 이전에 저장해둔 URL에서 뽑아내는 로직이 필요합니다.
        String cafe24ProductNo = extractCafe24No(product);

        if (cafe24ProductNoOpt.isPresent()) {
          // 2. 번호를 찾았다면, 해당 번호로 상세 조회 및 메모 갱신 로직을 태웁니다!
          singleSyncProcessor.syncWithCafe24(product.getSku(), cafe24ProductNoOpt.get());
          successCount++;
        } else {
          log.warn("상품 [{}]은(는) 카페24에 등록되지 않아 건너뜁니다.", product.getSku());
          notFoundCount++;
        }

        // ★ 중요: 카페24 API는 초당 2건 호출 제한(Rate Limit)이 빡빡합니다.
        // 검색(GET) -> 상세조회(GET) -> 메모수정(PUT) 으로 한 루프당 API를 3번이나 치기 때문에,
        // 여기서 강제로 5초(5000ms) 정도 휴식을 주어야 서버가 IP를 차단당하지 않습니다!
        Thread.sleep(5000);

      } catch (Exception e) {
        // 특정 상품 하나가 에러나도 전체 루프가 멈추면 안 됩니다!
        log.error("🚨 상품 [{}] 동기화 실패: {}", product.getSku(), e.getMessage());
        failCount++;
      }
    }

    log.info("🏁 [백그라운드 작업 종료] 총 시도: {}, 성공: {}, 실패: {}", allProducts.size(), successCount, failCount);
  }

  // 카페24 상품 번호를 유추하거나 매핑하는 헬퍼 메서드
  private String extractCafe24No(Product product) {
    // TODO: 사용자님의 실제 데이터 구조에 맞게 수정해주세요!
    // 임시 방편 1: memo 필드에 카페24 번호가 적혀 있다면?
    // 임시 방편 2: sourceUrl이 "https://younzara.cafe24.com/product/detail.html?product_no=123" 형태라면 정규식으로 번호만 추출!
    return "123456"; // 임시 더미 번호
  }
}