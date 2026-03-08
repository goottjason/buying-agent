/*
package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationReader;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationWriter;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.client.MarketClient;
import com.sbshop.agent.core.domain.product.client.dto.MarketExtractedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductItemSyncUseCase { // 🚀 Manager 대신 UseCase

  // 🚀 Reader/Writer 깔끔하게 주입
  private final ProductReader productReader;
  private final MarketRegistrationReader registrationReader;
  private final MarketRegistrationWriter registrationWriter;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void syncMatchedProduct(Product matchedProduct, String marketItemId, MarketExtractedData extractedData, MarketClient client) {
    MarketType marketType = client.getSupportedMarket();

    // 1. 최신 영속 객체 로드
    Product freshProduct = productReader.read(matchedProduct.getId());

    // 2. 마스터 데이터 업데이트
    if (extractedData.isMasterData()) {
      freshProduct.update(extractedData.toProductUpdateCommand());
    }

    // 3. 🚀 꼬리표 떼기 (엔티티 스스로 해결!)
    freshProduct.removeUnregisteredMark(marketType);
    log.info("   ✂️ 미등록 꼬리표 점검 완료 (SKU: {})", freshProduct.getSku());

    // 4. MarketRegistration Upsert
    Optional<MarketRegistration> optionalReg = registrationReader.findByProductAndMarket(freshProduct.getId(), marketType);

    if (optionalReg.isPresent()) {
      optionalReg.get().update(extractedData.toRegistrationUpdateCommand());
      log.info("   💾 기존 맵핑 정보 갱신 완료");
    } else {
      MarketRegistration newRegistration = MarketRegistration.create(
          freshProduct, marketType, extractedData.marketIdentifiers(), extractedData.rawData());
      registrationWriter.write(newRegistration);
      log.info("   💾 신규 맵핑 정보 저장 완료");
    }
  }

  public void deleteGhostProduct(String marketId, MarketClient client) {
    log.warn("   👻 유령 상품 마켓 삭제 요청. (마켓 ID: {})", marketId);
    client.deleteMarketProduct(marketId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markAsRequiresRegistration(List<Long> unmatchedProductIds, MarketType marketType) {
    List<Product> freshProducts = productReader.readAllByIds(unmatchedProductIds);

    for (Product product : freshProducts) {
      // 🚀 꼬리표 붙이기 (지저분한 JSON 파싱 코드가 단 한 줄로 컷!)
      product.markAsUnregistered(marketType);
      log.info("   📝 미등록 상품 꼬리표 추가 완료 (SKU: {})", product.getSku());
    }
  }
}*/
