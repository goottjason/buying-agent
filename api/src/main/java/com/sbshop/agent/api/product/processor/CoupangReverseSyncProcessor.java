package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationFinder;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationRecorder;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.coupang.adapter.CoupangProductAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoupangReverseSyncProcessor {

  private final CoupangProductAdapter coupangAdapter;
  private final ProductFinder productFinder;
  private final MarketRegistrationRecorder registrationRecorder;
  private final MarketRegistrationFinder registrationFinder;

  // 🚀 데이터가 3,000개면 시간이 오래 걸려 API 타임아웃이 날 수 있으므로 @Async로 비동기 실행을 권장합니다!
  // (설정상 @Async 사용이 어렵다면 빼셔도 로직은 동일하게 돌아갑니다)
  @Async
  public void runReverseMapping() {
    log.info("==================================================");
    log.info("🔄 [쿠팡 역방향 동기화] 대장정을 시작합니다! (우리 DB에 쿠팡 맵핑 데이터 꽂아넣기)");
    log.info("==================================================");

    // 1. 쿠팡에 있는 모든 상품 ID(sellerProductId) 싹쓸이
    List<String> sellerProductIds = coupangAdapter.fetchAllSellerProductIds();

    if (sellerProductIds == null || sellerProductIds.isEmpty()) {
      log.warn("⚠️ 쿠팡에서 가져올 상품 ID가 없습니다. API 권한이나 파라미터를 확인해주세요.");
      return;
    }

    int successCount = 0;
    int skipCount = 0;
    int failCount = 0;

    log.info("🚀 본격적인 단건 상세 조회 및 DB 맵핑 루프 진입! (총 {}건)", sellerProductIds.size());

    // 2. 각 ID별로 상세 정보를 가져와서 우리 DB에 꽂아넣기
    for (int i = 0; i < sellerProductIds.size(); i++) {
      String sellerProductId = sellerProductIds.get(i);
      log.info("--------------------------------------------------");
      log.info("⏳ [{}/{}] 타겟 쿠팡 상품 ID: {}", i + 1, sellerProductIds.size(), sellerProductId);

      try {
        // 🚨 CCTV 1: 쿠팡 API 통신 직전
        log.info("   ▶️ [CCTV-1] 쿠팡 API 단건 상세 조회 요청 (extractInitialProductData)...");
        MarketExtractedData extractedData = coupangAdapter.extractInitialProductData(sellerProductId);
        String sku = extractedData.marketIdentifiers().get("externalVendorSku");
        log.info("   ◀️ [CCTV-1] 쿠팡 API 응답 완료! (찾아낸 SKU: {})", sku);

        if (sku == null || sku.trim().isEmpty()) {
          log.warn("[{}/{}] ⚠️ SKU 누락 상품 스킵 (sellerProductId: {})", i + 1, sellerProductIds.size(), sellerProductId);
          failCount++;
          continue;
        }

        // 🚨 CCTV 2: 우리 DB 상품 조회 직전
        log.info("   ▶️ [CCTV-2] 우리 DB에서 상품 검색 시작 (findBySku)...");
        Optional<Product> optionalProduct = productFinder.findBySku(sku);
        log.info("   ◀️ [CCTV-2] 우리 DB 상품 검색 완료 (존재 여부: {})", optionalProduct.isPresent());

        // =========================================================================
        // 🚀 [신규] 카페24 우회 탐색 로직 (Fallback)
        // =========================================================================
        boolean isCafe24Fallback = false;
        if (optionalProduct.isEmpty() && sku.startsWith("P") && sku.length() >= 8) {
          // 예: "P000BAAA000A" -> "P000BAAA" (앞 8자리 추출)
          String cafe24ProductCode = sku.substring(0, 8);
          log.info("   ▶️ [CCTV-2.1] ⚠️ SKU 탐색 실패! 카페24 코드로 우회 탐색 시도 (추출된 코드: {})", cafe24ProductCode);

          // MarketRegistration의 CAFE24 식별자에서 product_code로 진짜 Product를 찾아옵니다!
          optionalProduct = registrationFinder.findProductByCafe24ProductCode(cafe24ProductCode);

          if (optionalProduct.isPresent()) {
            log.info("   ✅ [CCTV-2.1] 카페24 코드로 진짜 상품(진짜 SKU: {}) 찾기 대성공!", optionalProduct.get().getSku());
            isCafe24Fallback = true;
          }
        }
        // =========================================================================

        if (optionalProduct.isPresent()) {
          Product product = optionalProduct.get();
          String realSku = product.getSku(); // 우리 DB의 진짜 SKU

          // 🚀 [신규] 쿠팡 SKU 자동 교정 로직 (Auto-Correction)
          if (isCafe24Fallback) {
            String vendorItemId = extractedData.marketIdentifiers().get("vendorItemId");
            log.info("   ▶️ [CCTV-2.2] 🛠️ 쿠팡의 가짜 SKU({})를 진짜 SKU({})로 교정하는 API 호출!", sku, realSku);

            // 쿠팡 API를 찔러서 externalVendorSku를 진짜로 덮어씁니다.
            boolean updateSuccess = coupangAdapter.updateExternalVendorSku(vendorItemId, realSku);
            if (updateSuccess) {
              log.info("   ◀️ [CCTV-2.2] 쿠팡 SKU 교정 완료!");
            } else {
              log.warn("   ◀️ [CCTV-2.2] 쿠팡 SKU 교정 실패 (다음에 수동 변경 필요)");
            }
          }

          // =====================================================================
          // 🚀 1. 세련된 Product 부분 업데이트 (Command 패턴 활용)
          // =====================================================================
          // 개발자님이 구현하신 ProductUpdateCommand (혹은 비슷한 DTO) 구조를 가정했습니다.
          // null인 필드는 엔티티 내부 update 메서드에서 무시되도록 설계되어 있다고 전제합니다.
          // 🚨 CCTV 3: 엔티티 업데이트 (메모리)
          log.info("   ▶️ [CCTV-3] Product 엔티티 업데이트 수행 (product.update)...");

          ProductUpdateCommand updateCommand = ProductUpdateCommand.builder()
              .brand(extractedData.brand())
              .manufacturer(extractedData.manufacturer())
              .baseName(extractedData.generalProductName()) // 🚀 쿠팡의 순수 상품명을 마스터 데이터로 흡수!
              .barcode(extractedData.barcode())
              .build();

          // 도메인 메서드 호출! (JPA 더티 체킹 발동)
          product.update(updateCommand);
          // =====================================================================
          log.info("   ◀️ [CCTV-3] Product 엔티티 업데이트 완료");

          // 🚨 CCTV 4: 기존 매핑 여부 조회
          log.info("   ▶️ [CCTV-4] 기존 MarketRegistration 맵핑 여부 조회 (findByProductIdAndMarketType)...");
          boolean isAlreadyMapped = registrationFinder.findByProductIdAndMarketType(product.getId(), MarketType.COUPANG).isPresent();
          log.info("   ◀️ [CCTV-4] 맵핑 여부 확인 완료 (isAlreadyMapped: {})", isAlreadyMapped);

          // =====================================================================
          // 🚀 2. 쿠팡 식별자 바구니 MarketRegistration 영구 저장
          // =====================================================================

          if (!isAlreadyMapped) {
            registrationRecorder.recordSyncSuccess(
                product,
                MarketType.COUPANG,
                extractedData.marketIdentifiers(), // vendorItemId 등 7종 세트
                extractedData.rawData()
            );
            log.info("[{}/{}] ✅ 맵핑 & Product 업데이트 성공! (SKU: {} -> 쿠팡: {})", i + 1, sellerProductIds.size(), sku, sellerProductId);
            successCount++;
          } else {
            log.info("[{}/{}] ⏩ 매핑은 이미 존재함 (Product 빈칸 업데이트만 수행) (SKU: {})", i + 1, sellerProductIds.size(), sku);
            skipCount++;
          }
        }
        else {
          failCount++;
        }

        Thread.sleep(1000); // 쿠팡 API 방어

      } catch (Exception e) {
        log.error("[{}/{}] ❌ 통신/저장 실패 (sellerProductId: {}): {}", i + 1, sellerProductIds.size(), sellerProductId, e.getMessage());
        failCount++;
      }
    }

    log.info("==================================================");
    log.info("🏁 [쿠팡 역방향 동기화 완료] 총 시도: {}, 맵핑 성공: {}, 실패/스킵: {}", sellerProductIds.size(), successCount, failCount);
    log.info("==================================================");
  }
}