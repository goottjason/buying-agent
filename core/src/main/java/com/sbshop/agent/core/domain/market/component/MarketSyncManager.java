package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSyncManager {

  private final MarketRegistrationFinder registrationFinder;
  private final MarketRegistrationRecorder registrationRecorder;

  // 🚀 [A ∩ B] 교집합 처리: 맵핑 및 알짜 데이터 마스터 업데이트
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void syncMatchedProduct(Product product, String marketId, MarketExtractedData data, MarketType marketType, MarketSyncPort adapter) {
    // 1. 도메인 업데이트 (Command 패턴)
    ProductUpdateCommand updateCommand = ProductUpdateCommand.builder()
        .brand(data.brand())
        .manufacturer(data.manufacturer())
        .baseName(data.generalProductName())
        .barcode(data.barcode())
        .build();
    product.update(updateCommand);

    // 2. 가짜 SKU 교정 (카페24 Fallback을 통해 찾았고, 마켓 SKU와 실제 SKU가 다를 경우)
    String marketSku = data.marketIdentifiers().get("externalVendorSku");
    if (marketSku != null && !marketSku.equals(product.getSku())) {
      String vendorItemId = data.marketIdentifiers().get("vendorItemId");
      adapter.updateExternalVendorSku(vendorItemId, product.getSku()); // 마켓 API 호출
      log.info("   🛠️ 마켓 SKU 교정 완료: {} -> {}", marketSku, product.getSku());
    }

    // 3. 맵핑 영구 저장
    boolean isMapped = registrationFinder.findByProductIdAndMarketType(product.getId(), marketType).isPresent();
    if (!isMapped) {
      registrationRecorder.recordSyncSuccess(product, marketType, data.marketIdentifiers(), data.rawData());
      log.info("   ✅ 맵핑 및 동기화 완료 (SKU: {})", product.getSku());
    }
  }

  // 🚀 [B - A] 유령 상품 처리: 마켓에서 삭제 호출
  public void deleteGhostProduct(String marketId, MarketSyncPort adapter) {
    log.warn("   👻 유령 상품 발견! 마켓에서 삭제합니다. (마켓 ID: {})", marketId);
    adapter.deleteMarketProduct(marketId);
  }

  // 🚀 [A - B] 미등록 상품 처리: Memo 마킹
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markAsRequiresRegistration(List<Product> unmatchedProducts, MarketType marketType) {
    String keyword = "[추가등록필요] " + marketType.name();

    for (Product product : unmatchedProducts) {
      String currentMemo = product.getMemo() != null ? product.getMemo() : "";
      if (!currentMemo.contains(keyword)) {
        String newMemo = currentMemo.isEmpty() ? keyword : currentMemo + "\n" + keyword;

        // 단일 update 창구 활용
        product.update(ProductUpdateCommand.builder().memo(newMemo).build());
        log.info("   📝 미등록 상품 마킹 완료 (SKU: {} -> 대상 마켓: {})", product.getSku(), marketType);
      }
    }
  }
}