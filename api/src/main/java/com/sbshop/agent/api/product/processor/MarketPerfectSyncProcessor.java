package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationFinder;
import com.sbshop.agent.core.domain.market.component.MarketSyncManager;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.model.LocalProductDictionary;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPerfectSyncProcessor {

  // 실제 구현체인 쿠팡, 11번가 등 어댑터는 스프링이 시작될 때 이 List 안에 전부 주입됨
  private final List<MarketSyncPort> marketPorts;
  private final ProductFinder productFinder;
  private final MarketRegistrationFinder marketFinder;
  private final MarketSyncManager syncManager;

  @Async
  public void runPerfectSync(MarketType targetMarket) {

    log.info("==================================================");
    log.info(" [완벽 동기화 시작] 타겟 마켓: {}", targetMarket);
    log.info("==================================================");

    // 1. 타겟 마켓 어댑터 찾기
    MarketSyncPort adapter = marketPorts.stream()
        .filter(port -> port.getSupportedMarket() == targetMarket)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 마켓입니다."));

    // 2. sku 및 code(cafe24)로 Map 생성
    List<Product> allLocalProducts = productFinder.findAllProducts();
    List<MarketRegistration> allCafe24Registrations = marketFinder.findAllByMarketType(MarketType.CAFE24);
    LocalProductDictionary dictionary = new LocalProductDictionary(allLocalProducts, allCafe24Registrations);

    // 3. 타겟 마켓의 등록된 모든 상품 ID 추출하여 List 생성
    // COUPANG(sellerProductId), SMARTSTORE(channelProductNo 또는 originProductNo)
    // ELEVENST(prdNo), CAFE24(product_no) (예: "15463483670")
    List<String> marketIds = adapter.fetchAllMarketProductIds();

    // 4. [A ∩ B] 교집합 맵핑 및 [B - A] 유령상품 삭제 루프
    for (String marketId : marketIds) {
      try {
        // 쿠팡 API를 찔러서 거대한 JSON을 파싱한 뒤, 우리가 쓰기 편한 MarketExtractedData DTO로 예쁘게 포장해서 돌려받습니다.
        MarketExtractedData data = adapter.extractProductData(marketId);

        // 🚀 [개선 포인트 1] 하드코딩 제거!
        // 마켓별 고유한 식별자 이름(externalVendorSku 등)을 알 필요 없이,
        // 어댑터가 친절하게 세팅해준 '공통 열쇠'만 쏙 빼서 씁니다.
        String mappingKey = data.mappingKey();

        // 유령 상품 방어 1: 마켓에 등록된 상품인데, 매칭할 열쇠조차 비어있다면 쓰레기 데이터입니다.
        if (mappingKey == null || mappingKey.trim().isEmpty()) {
          log.warn("   👻 열쇠(SKU/Code)가 비어있는 쓰레기 상품 발견! 삭제합니다. (마켓 ID: {})", marketId);
          syncManager.deleteGhostProduct(marketId, adapter);
          continue;
        }

        // 🚀 [개선 포인트 2] 변수명 및 의미 명확화!
        // 이 mappingKey는 진짜 SKU일 수도 있고, 카페24 우회 코드(P000...)일 수도 있습니다.
        // 일급 컬렉션(Dictionary)에게 이 열쇠를 던져주면, 내부적으로 두 개의 맵을 다 뒤져서 기가 막히게 진짜 Product를 찾아옵니다.
        Optional<Product> matchedProduct = dictionary.findAndMarkAsMatched(mappingKey);

        if (matchedProduct.isPresent()) {
          // 👉 교집합: 맵핑 및 업데이트
          // 팡에서 가져온 브랜드/바코드 등으로 우리 Product의 빈칸을 채워 넣고,
          // MarketRegistration에 쿠팡 고유 식별자(7종 세트)를 영구 보존하고,
          //     혹시 마켓 SKU가 카페24 흔적(P000BAAA000A)이었다면, 쿠팡 API를 찔러서 진짜 우리 SKU(250401IHB025)로 **교정(Update)**까지 해버립니다!
          syncManager.syncMatchedProduct(matchedProduct.get(), marketId, data, targetMarket, adapter);
        } else {
          // 👉 차집합(마켓에만 존재): 유령 상품 삭제
          // 사전을 아무리 뒤져도(카페24 우회까지 다 해봐도) 우리 DB에 없는 상품입니다. 즉, 과거에 등록해 놓고 지웠거나, 누군가 수동으로 등록한 **'유령 상품(쓰레기 데이터)'**
          // 마켓 API에 "이 상품 삭제해!"(DELETE 호출) 하고 휴지통으로 날려버립니다. 마켓을 아주 클린하게 청소
          syncManager.deleteGhostProduct(marketId, adapter);
        }

        Thread.sleep(1000); // API Rate Limit 방어
      } catch (Exception e) {
        log.error("❌ 처리 중 오류 발생 (ID: {}): {}", marketId, e.getMessage());
      }
    }

    // 5. [A - B] 미등록 상품 색출 및 메모 마킹
/*
    내부 동작 (비유하자면 '출석부 확인'): 아까 2번 과정에서 dictionary 안에는 우리 DB의 모든 상품(예: 10,000개)이 들어있었습니다.
    그리고 4번 루프를 돌면서 쿠팡과 매칭된 상품(예: 2,500개)은 matchedSkus라는 출석부에 도장을 쾅쾅 찍어뒀죠.
    이제 getUnmatchedProducts()를 부르면, 10,000개 중에서 출석 도장이 없는 7,500개의 상품만 쏙 골라서 리스트로 돌려줍니다. 이 7,500개가 바로 "쿠팡에 아직 안 올라간 신상(또는 누락) 상품들"입니다!*/
    List<Product> unmatchedProducts = dictionary.getUnmatchedProducts();
    // "매니저야, 방금 뽑아온 7,500개의 미등록 상품들 이마에다가 **'[추가등록필요] COUPANG' 이라는 꼬리표(Memo)**를 단체로 붙여버려!"
    syncManager.markAsRequiresRegistration(unmatchedProducts, targetMarket);

    log.info("🏁 [{} 완벽 동기화 프로세스 종료]", targetMarket);
    log.info("==================================================");
  }
}