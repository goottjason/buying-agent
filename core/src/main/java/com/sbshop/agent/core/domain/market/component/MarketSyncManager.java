package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import java.util.Optional;
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
  private final MarketRegistrationRemover registrationRemover;

  // 🚀 [A ∩ B] 교집합 처리: 맵핑 및 알짜 데이터 마스터 업데이트
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void syncMatchedProduct(Product matchedProduct,
      String marketProductId, // 🚀 루프에서 쓰던 마켓의 식별자(ID)를 그대로 넘겨받음
      MarketExtractedData extractedData,
      MarketSyncPort adapter) {

    MarketType marketType = adapter.getSupportedMarket();

    // 🚀 1. Product(상품 마스터) 정보 업데이트
    if (extractedData.isMasterData()) {
      matchedProduct.update(extractedData.toProductUpdateCommand());
    }

    // ====================================================================
    // 🚀 2 & 3. MarketRegistration Upsert (있으면 수정, 없으면 신규 생성)
    // ====================================================================
    Optional<MarketRegistration> optionalReg = registrationFinder.findByProductIdAndMarketType(matchedProduct.getId(), marketType);

    if (optionalReg.isPresent()) {
      // [케이스 A] 이미 맵핑된 기록이 있으면 최신 정보로 덮어쓰기 (Update)
      MarketRegistration registration = optionalReg.get();
      registration.update(extractedData.toRegistrationUpdateCommand());
      log.info("   🔄 기존 맵핑 정보 갱신 완료 (SKU: {}, 마켓: {})", matchedProduct.getSku(), marketType);
    } else {
      // [케이스 B] 최초 발견! 신규 맵핑 정보 영구 저장 (Insert - 개발자님이 질문하신 부분!)
      registrationRecorder.recordSyncSuccess(matchedProduct, marketType, extractedData.marketIdentifiers(), extractedData.rawData());
      log.info("   ✅ 신규 맵핑 및 동기화 완료 (SKU: {}, 마켓: {})", matchedProduct.getSku(), marketType);
    }

    // ====================================================================
    // 🚀 4. 원본 마켓의 가짜 SKU 원격 교정 (Market-Agnostic)
    // ====================================================================
    checkAndCorrectFakeSku(matchedProduct.getSku(), extractedData.mappingKey(), marketProductId, adapter);
  }

  /**
   * [내부 헬퍼] 마켓 서버의 잘못된 SKU를 진짜 SKU로 교정 요청
   */
  private void checkAndCorrectFakeSku(String realSku, String marketKey, String marketProductId, MarketSyncPort adapter) {
    if (marketKey != null && !marketKey.equals(realSku)) {
      log.info("🛠️ 마켓의 잘못된 SKU({}) 감지! 마켓 서버에 진짜 SKU({})로 교정을 요청합니다.", marketKey, realSku);
      // 쿠팡이면 API를 쏴서 고칠 것이고, 다른 마켓은 default 메서드로 인해 무시됨!
      adapter.correctMarketSku(marketProductId, realSku);
    }
  }

  // 🚀 [B - A] 유령 상품 처리: 마켓에서 삭제 호출
  public void deleteGhostProduct(String marketId, MarketSyncPort adapter) {
    log.warn("   👻 유령 상품 발견! 마켓에서 삭제합니다. (마켓 ID: {})", marketId);
    // 1. 마켓에서 먼저 삭제
    boolean isDeleted = adapter.deleteMarketProduct(marketId);

    // 2. 혹시라도 우리 DB(MarketRegistrations)에 쓰레기 데이터로 남아있으면 같이 지움
    if (isDeleted) {
      registrationRemover.deleteByMarketProductId(marketId);
    }
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