package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationFinder;
import com.sbshop.agent.core.domain.market.component.MarketSyncManager;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.model.LocalProductDictionary;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketAdapterRouter;
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
public class PerfectSyncProcessor {

  private final MarketAdapterRouter adapterRouter;
  private final ProductFinder productFinder;
  private final MarketRegistrationFinder marketFinder;
  private final MarketSyncManager syncManager;

  @Async
  public void runPerfectSync(MarketType targetMarket) {

    log.info("▶️ [{} 완벽 동기화 프로세스 시작]", targetMarket);

    // 타겟 마켓 어댑터 찾기
    MarketSyncPort adapter = adapterRouter.getAdapter(targetMarket);

    // 모든 Product 반환 (List)
    List<Product> allProducts = productFinder.findAllProducts();
    log.info("✅ 전체 Product 목록 조회 완료 (총 {}건)", allProducts.size());

    // CAFE24로 등록된 MarketRegistration 반환 (List) (임시코드)
    List<MarketRegistration> allCafe24Registrations = marketFinder.findAllByMarketType(MarketType.CAFE24);
    log.info("✅ CAFE24 마켓 등록 정보 조회 완료 (총 {}건)", allCafe24Registrations.size());

    // ----- allProducts를 순회하여 skuMap 생성, allCafe24Registrations를 순회하여 cafe24CodeMap 생성
    LocalProductDictionary dictionary = new LocalProductDictionary(allProducts, allCafe24Registrations);
    log.info("✅ LocalProductDictionary 생성 완료 완료");

    // COUPANG:  서버에 등록된 모든 아이템의 sellerProductId를 모아둔 리스트
    // SMARTSTORE: 마켓 서버에 등록된 모든 아이템의 originProductNo 모아둔 리스트
    // ELEVENST: 마켓 서버에 등록된 모든 아이템의 prdNo 모아둔 리스트
    // CAFE24: 마켓 서버에 등록된 모든 아이템의 product_no 모아둔 리스트
    List<String> marketItemIds = adapter.fetchAllMarketItemIds();
    log.info("✅ {} 아이템 식별자 ID 목록 조회 완료 (총 {}건 발견)", targetMarket, marketItemIds.size());

    // ====================================================================
    // 🚀 [테스트용 임시 코드] 여기서 메서드를 즉시 종료시킵니다!
    // ====================================================================
    // log.info("🚧 [TEST] 아이템 수집까지만 진행하고 프로세스를 강제 종료합니다.");
    // if (true) {
    //   log.info("🚧 [TEST] 아이템 수집까지만 진행하고 프로세스를 강제 종료합니다.");
    //   return;
    // }

    int totalItems = marketItemIds.size();
    int currentIndex = 1;

    // 마켓 서버에 있는 아이템을 순회
    for (String marketItemId : marketItemIds) {
      log.info("🔄 [{}/{}] 마켓 아이템 처리 시작 (Market ID: {})", currentIndex++, totalItems, marketItemId);

      try {

        // COUPANG: sellerProductId로 조회된 아이템의 데이터를 파싱하여 MarketExtractedData로 조립
        // SMARTSTORE: originProductNo로 조회된 아이템의 데이터를 파싱하여 MarketExtractedData로 조립
        // ELEVENST: prdNo로 조회된 아이템의 데이터를 파싱하여 MarketExtractedData로 조립
        // CAFE24: product_no로 조회된 아이템의 데이터를 파싱하여 MarketExtractedData로 조립
        MarketExtractedData data = adapter.extractProductData(marketItemId);
        // log.info("   ✅ 상품 상세 데이터 추출 및 파싱 완료");

        // COUPANG: externalVendorSku
        // SMARTSTORE: sellerCustomCode1
        // ELEVENST: SellerPrdCd
        // CAFE24: custom_product_code
        String mappingKey = data.mappingKey();
        log.info("   🔑 추출된 매핑 키(Mapping Key): [{}]", mappingKey);

        if (mappingKey == null || mappingKey.trim().isEmpty()) {
          // TODO: 비어있다고 삭제하는 건 성급함. 비어있음을 마켓 서버의 아이템에 표시했다가 추후에 원인을 파악해보는게 좋을 듯
          log.warn("   ⚠️ [보류] 매핑 키가 비어있습니다. 안전을 위해 삭제하지 않고 스킵합니다.");
          // syncManager.deleteGhostProduct(marketItemId, adapter);
          continue;
        }

        // COUPANG: externalVendorSku로 Product 찾고,
        // SMARTSTORE: sellerCustomCode1
        // ELEVENST: SellerPrdCd
        // CAFE24: custom_product_code
        Optional<Product> matchedProduct = dictionary.findAndMarkAsMatched(mappingKey);

        // 마켓 서버에 조회한 아이템이 우리 Product에 있음
        if (matchedProduct.isPresent()) {
          // Product의 빈칸을 채울 필드 채우고, MarketRegistration에 채울 필드 채움
          // 쿠팡의 경우, API 호출하여 제대로된 sku로 아이템 수정하는 작업
          Product product = matchedProduct.get();
          log.info("   🟢 [매칭 성공] 매핑키[{}]로 로컬 상품 발견! (SKU: {})", mappingKey, product.getSku());

          // Product의 빈칸을 채울 필드 채우고, MarketRegistration에 채울 필드 채움
          // 쿠팡의 경우, API 호출하여 제대로된 sku로 아이템 수정하는 작업
          syncManager.syncMatchedProduct(product, marketItemId, data, adapter);
          // log.info("   ✅ 교집합 동기화 처리 완료");
          String sku = product.getSku();
          if (mappingKey != null && !mappingKey.equals(sku)) {
            log.info("🛠️ 마켓의 잘못된 SKU({}) 감지! 교정을 요청 (쿠팡만 작동됨)", mappingKey);
            // ====================================================================
            // 🚀 4. 원본 마켓의 가짜 SKU 원격 교정 (Market-Agnostic)
            // ====================================================================
            adapter.correctMarketSku(marketItemId, sku);
          }
        }
        // 마켓 서버에 있는 아이템이 우리 Product에 없음 (ex. 예전에 올렸으나 Product에서 없앴으나 마켓 서버에 미처 지우지 못한 상품들)
        else {
          log.warn("   🔴 [매칭 실패] 로컬 DB에 존재하지 않는 유령 상품입니다. 차집합(B - A) 삭제 로직을 시작합니다.");
          syncManager.deleteGhostProduct(marketItemId, adapter);
          log.info("   🛠️ 마켓 유령 상품 삭제 요청 완료");
        }

        // log.info("   ⏳ 수동 확인을 위해 1분(60,000ms)간 일시 정지합니다...");
        Thread.sleep(500);
        // log.info("   ⏰ 1초 대기 종료. 다음 상품 처리를 준비합니다.");

      } catch (Exception e) {
        log.error("❌ 처리 중 오류 발생 (ID: {}): {}", marketItemId, e.getMessage());
      } finally {
        // 🚀 [핵심 방어벽] 성공하든, continue로 스킵하든, 에러가 나든 무조건 1초 대기!
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }

    log.info("--------------------------------------------------");
    log.info("👉 모든 마켓 아이템 순회가 끝났습니다. 미등록 상품(A - B) 색출을 시작합니다...");

    // 모든 마켓을 순회 후에 바구니에 담겨있지 않다면, 마켓 서버에 올라가지 않은 상품들이므로 추후에 재등록 한 뒤 싱크를 맞춰야 함
    List<Product> unmatchedProducts = dictionary.getUnmatchedProducts();
    log.info("✅ 미등록 상품 색출 완료 (총 {}건 발견)", unmatchedProducts.size());

    if (!unmatchedProducts.isEmpty()) {
      // 🚀 [수정] 무거운 객체 대신 ID(Long) 식별자만 뽑아서 매니저에게 던집니다!
      List<Long> unmatchedProductIds = unmatchedProducts.stream()
          .map(Product::getId)
          .toList();
      log.info("👉 미등록 상품들에 대해 꼬리표(JSON Memo) 마킹을 진행합니다...");
      // "매니저야, 방금 뽑아온 미등록 상품들 이마에다가 **'[추가등록필요] COUPANG' 이라는 꼬리표(Memo)**를 단체로 붙여버려!"
      syncManager.markAsRequiresRegistration(unmatchedProductIds, targetMarket); // 🚀 ID 리스트 전달
      log.info("✅ 꼬리표 마킹 완료");
    } else {
      log.info("✅ 로컬의 모든 상품이 마켓에 완벽하게 등록되어 있습니다! (미등록 0건)");
    }

    log.info("==================================================");
    log.info("🏁 [{} 완벽 동기화 프로세스 전체 종료]", targetMarket);
    log.info("==================================================");
  }
}