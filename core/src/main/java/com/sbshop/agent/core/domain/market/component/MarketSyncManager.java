package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.common.port.JsonUtilPort;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSyncManager {

  private final JsonUtilPort jsonUtil; // 🚀 ObjectMapper 대신 Port 주입!
  private final ProductFinder productFinder;
  private final MarketRegistrationFinder registrationFinder;
  private final MarketRegistrationAppender registrationAppender;

  // 🚀 [A ∩ B] 교집합 처리: 맵핑 및 알짜 데이터 마스터 업데이트
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void syncMatchedProduct(
      Product matchedProduct, // 🚨 주의: 이 객체는 영속성이 끊어진(Detached) 상태입니다!
      String marketItemId, // 🚀 루프에서 쓰던 마켓의 식별자(ID)를 그대로 넘겨받음
      MarketExtractedData extractedData,
      MarketSyncPort adapter
  ) {

    MarketType marketType = adapter.getSupportedMarket();

    // ====================================================================
    // 🚀 0. 트랜잭션 내부에서 최신 상태의 영속(Managed) 객체로 다시 불러옵니다!
    // ====================================================================
    // (productFinder에 findById 메서드가 Optional을 반환한다면 .orElseThrow() 처리 해주세요)
    Product freshProduct = productFinder.findById(matchedProduct.getId())
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + matchedProduct.getId()));


    // 🚀 1. Product(상품 마스터) 정보 업데이트
    if (extractedData.isMasterData()) {
      freshProduct.update(extractedData.toProductUpdateCommand());
    }

    // ====================================================================
    // 🚀 [추가] 1-5. 마켓 동기화가 확인되었으므로 '미등록' 꼬리표를 떼어냅니다 (자가 치유)
    // ====================================================================
    String currentMemo = freshProduct.getMemo();
    if (currentMemo != null && !currentMemo.isBlank()) {
      String updatedMemo = removeUnregisteredMarketFromJson(currentMemo, marketType);

      if (!currentMemo.equals(updatedMemo)) {
        // 값이 변경되었다면 부분 업데이트 커맨드를 통해 메모만 살짝 덮어씌웁니다.
        freshProduct.update(ProductUpdateCommand.builder().memo(updatedMemo).build());
        log.info("   ✂️ 동기화 확인! 미등록 꼬리표 제거 완료 (SKU: {} -> {})", freshProduct.getSku(), marketType);
      }
    }

    // ====================================================================
    // 🚀 2 & 3. MarketRegistration Upsert (있으면 수정, 없으면 신규 생성)
    // ====================================================================
    Optional<MarketRegistration> optionalReg = registrationFinder.findByProductIdAndMarketType(matchedProduct.getId(), marketType);

    if (optionalReg.isPresent()) {
      // [케이스 A] 이미 맵핑된 기록이 있으면 최신 정보로 덮어쓰기 (Update)
      optionalReg.get().update(extractedData.toRegistrationUpdateCommand());
      log.info("   💾 기존 맵핑 정보 갱신 완료 (SKU: {}, 마켓: {})", matchedProduct.getSku(), marketType);
    } else {
      // [케이스 B] 최초 발견! 신규 맵핑 정보 영구 저장 (Insert - 개발자님이 질문하신 부분!)
      MarketRegistration newRegistration = MarketRegistration.create(
          matchedProduct, marketType, extractedData.marketIdentifiers(), extractedData.rawData());
      registrationAppender.save(newRegistration);
      log.info("   💾 신규 맵핑 및 동기화 완료 (SKU: {}, 마켓: {})", matchedProduct.getSku(), marketType);
    }
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
    adapter.deleteMarketProduct(marketId);
  }

  // 🚀 [A - B] 미등록 상품 처리: Memo 마킹
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markAsRequiresRegistration(List<Long> unmatchedProductIds,MarketType marketType) {
    // 🚀 1. 트랜잭션 내부에서 최신 상태의 영속(Managed) 객체들로 싹 다 다시 불러옵니다!
    // (ProductFinder에 findAllByIds 같은 메서드가 없다면 하나 만들어주세요)
    List<Product> freshProducts = productFinder.findAllByIds(unmatchedProductIds);

    for (Product product : freshProducts) {
      String currentMemo = product.getMemo() != null ? product.getMemo() : "";

      // JSON 조작 헬퍼 호출
      String updatedMemo = addUnregisteredMarketToJson(currentMemo, marketType);

      if (!currentMemo.equals(updatedMemo)) {
        product.update(ProductUpdateCommand.builder().memo(updatedMemo).build());
        log.info("   📝 미등록 상품 JSON 꼬리표 추가 완료 (SKU: {} -> {})", product.getSku(), marketType);
      } else {
        // 🚀 이미 꼬리표가 있어서 스킵되었다는 것을 로그로 확인!
        log.debug("   ⏩ 이미 꼬리표가 존재하여 업데이트를 스킵합니다. (SKU: {})", product.getSku());
      }
    }
  }


  /**
   * [내부 헬퍼] 메모를 JSON Map으로 파싱하여 '미등록' 배열에 마켓을 추가합니다.
   */
  private String addUnregisteredMarketToJson(String memo, MarketType marketType) {
    try {
      Map<String, Object> memoMap = parseMemoToJson(memo);

      // '미등록' 리스트를 가져오거나 없으면 새로 만듭니다.
      @SuppressWarnings("unchecked")
      List<String> unregisteredMarkets = (List<String>) memoMap.computeIfAbsent("미등록", k -> new ArrayList<>());

      // 마켓이 리스트에 없다면 추가합니다. (중복 방지)
      if (!unregisteredMarkets.contains(marketType.name())) {
        unregisteredMarkets.add(marketType.name());
        // 다시 JSON 문자열로 변환하여 반환
        return jsonUtil.toJsonString(memoMap); // 🚀 Port 사용
      }

      return memo; // 변경사항이 없으면 원본 그대로 반환

    } catch (Exception e) {
      log.error("메모 JSON 파싱/업데이트 중 오류 발생. 원본 메모 유지: {}", memo, e);
      return memo;
    }
  }

  /**
   * [내부 헬퍼] 평문 메모에 대한 하위 호환성을 유지하며 Map으로 파싱합니다.
   */
  private Map<String, Object> parseMemoToJson(String memo) {
    if (memo == null || memo.isBlank()) return new HashMap<>();
    try {
      return jsonUtil.parseToMap(memo); // 🚀 Port 사용
    } catch (Exception e) {
      Map<String, Object> legacyMap = new HashMap<>();
      legacyMap.put("userMemo", memo);
      return legacyMap;
    }
  }

  /**
   * [내부 헬퍼] 메모의 JSON Map에서 특정 마켓의 '미등록' 꼬리표를 제거합니다.
   */
  private String removeUnregisteredMarketFromJson(String memo, MarketType marketType) {
    if (memo == null || memo.isBlank()) return memo;

    try {
      Map<String, Object> memoMap = parseMemoToJson(memo);

      if (memoMap.containsKey("미등록")) {
        @SuppressWarnings("unchecked")
        List<String> unregisteredMarkets = (List<String>) memoMap.get("미등록");

        // 리스트에 해당 마켓이 존재하면 삭제
        if (unregisteredMarkets != null && unregisteredMarkets.contains(marketType.name())) {
          unregisteredMarkets.remove(marketType.name());

          // 만약 미등록 마켓이 하나도 안 남았다면 '미등록' 키 자체를 깔끔하게 날려버립니다!
          if (unregisteredMarkets.isEmpty()) {
            memoMap.remove("미등록");
          }

          // 변경된 Map을 다시 JSON 문자열로 변환하여 반환
          return jsonUtil.toJsonString(memoMap); // 🚀 Port 사용
        }
      }
      return memo; // 변경사항이 없으면 원본 반환
    } catch (Exception e) {
      log.error("메모 JSON 파싱/제거 중 오류 발생. 원본 메모 유지: {}", memo, e);
      return memo;
    }
  }
}