/*
package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationReader;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.model.LocalProductDictionary;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.client.MarketClientRouter;
import com.sbshop.agent.core.domain.product.client.dto.MarketExtractedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncUseCase {

  private final MarketClientRouter clientRouter;
  private final ProductReader productReader;
  private final MarketRegistrationReader registrationReader;
  private final ProductItemSyncUseCase itemSyncUseCase; // 🚀 Manager 대신 UseCase

  @Async
  public void runPerfectSync(MarketType targetMarket) {
    log.info("▶️ [{} 완벽 동기화 프로세스 시작]", targetMarket);

    MarketClient client = clientRouter.getClient(targetMarket);

    List<Product> allProducts = productReader.readAll();
    List<MarketRegistration> allCafe24Registrations = registrationReader.readAllByMarketType(MarketType.CAFE24);

    LocalProductDictionary dictionary = new LocalProductDictionary(allProducts, allCafe24Registrations);
    List<String> marketItemIds = client.fetchAllMarketItemIds();

    int totalItems = marketItemIds.size();
    int currentIndex = 1;

    for (String marketItemId : marketItemIds) {
      log.info("🔄 [{}/{}] 마켓 아이템 처리 시작 (Market ID: {})", currentIndex++, totalItems, marketItemId);

      try {
        MarketExtractedData data = client.extractProductData(marketItemId);
        String mappingKey = data.mappingKey();

        if (mappingKey == null || mappingKey.trim().isEmpty()) {
          log.warn("   ⚠️ 매핑 키 비어있음. 스킵합니다.");
          continue;
        }

        Optional<Product> matchedProduct = dictionary.findAndMarkAsMatched(mappingKey);

        if (matchedProduct.isPresent()) {
          Product product = matchedProduct.get();
          log.info("   🟢 로컬 상품 매칭 성공! (SKU: {})", product.getSku());

          itemSyncUseCase.syncMatchedProduct(product, marketItemId, data, client);

          if (!mappingKey.equals(product.getSku())) {
            client.correctMarketSku(marketItemId, product.getSku());
          }
        } else {
          itemSyncUseCase.deleteGhostProduct(marketItemId, client);
        }
        Thread.sleep(500);

      } catch (Exception e) {
        log.error("❌ 처리 중 오류 발생 (ID: {}): {}", marketItemId, e.getMessage());
      } finally {
        try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
      }
    }

    log.info("👉 미등록 상품(A - B) 색출을 시작합니다...");
    List<Product> unmatchedProducts = dictionary.getUnmatchedProducts();

    if (!unmatchedProducts.isEmpty()) {
      List<Long> unmatchedProductIds = unmatchedProducts.stream().map(Product::getId).toList();
      itemSyncUseCase.markAsRequiresRegistration(unmatchedProductIds, targetMarket);
    } else {
      log.info("✅ 로컬의 모든 상품이 마켓에 완벽 등록되어 있습니다!");
    }

    log.info("🏁 [{} 완벽 동기화 프로세스 전체 종료]", targetMarket);
  }
}*/
