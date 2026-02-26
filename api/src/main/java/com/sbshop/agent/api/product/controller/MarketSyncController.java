package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.processor.CoupangReverseSyncProcessor;
import com.sbshop.agent.api.product.processor.MarketBatchSyncProcessor;
import com.sbshop.agent.api.product.processor.MarketPerfectSyncProcessor;
import com.sbshop.agent.api.product.processor.ProductSyncProcessor;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class MarketSyncController {

  private final MarketPerfectSyncProcessor marketPerfectSyncProcessor;
  private final ProductSyncProcessor productSyncProcessor;
  private final MarketBatchSyncProcessor batchSyncProcessor;
  private final CoupangReverseSyncProcessor coupangReverseSyncProcessor;

  @PostMapping("/{marketType}/perfect")
  public ResponseEntity<String> triggerPerfectSync(@PathVariable MarketType marketType) {
    log.info("API 호출 수신: {} 마켓 완벽 동기화 시작", marketType);
    marketPerfectSyncProcessor.runPerfectSync(marketType);
    return ResponseEntity.ok("✅ " + marketType + " 마켓 완벽 동기화 백그라운드 작업이 시작되었습니다.");
  }

  /**
   * [신규] 특정 SKU 단건 동기화 테스트 API
   * POST http://localhost:8080/api/admin/sync/cafe24/SKU-12345
   */
  @PostMapping("/{marketType}/{sku}")
  public ResponseEntity<?> syncSingleProduct(
      @PathVariable("marketType") String marketTypeStr,
      @PathVariable("sku") String sku
  ) {
    MarketType marketType = MarketType.valueOf(marketTypeStr.toUpperCase());
    log.info("API 호출 수신: {} 마켓 단건 동기화 테스트 - SKU: {}", marketType, sku);

    // 단건 프로세서를 호출합니다. (테스트용이므로 비동기 말고 바로 실행)
    productSyncProcessor.syncMarketProduct(sku, marketType);

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", String.format("[%s] 마켓의 [%s] 상품 JSON 데이터가 백엔드 콘솔에 출력되었습니다.", marketType.name(), sku)
    ));
  }


  /**
   * 범용 마켓 전체 동기화 트리거
   * POST /api/admin/sync/{marketType}/all
   * 예: POST /api/admin/sync/cafe24/all
   * 예: POST /api/admin/sync/smartstore/all
   */
  @PostMapping("/{marketType}/all")
  public ResponseEntity<?> syncAll(@PathVariable("marketType") String marketTypeStr) {
    // 1. 문자열을 Enum으로 변환 (예: "cafe24" -> CAFE24)
    MarketType marketType = MarketType.valueOf(marketTypeStr.toUpperCase());

    log.info("API 호출 수신: {} 전체 동기화 트리거 작동", marketType);

    // 2. 프로세서에 마켓 타입을 던지기만 하면 끝!
    batchSyncProcessor.syncAllProductsSlowly(marketType);

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", String.format("백그라운드에서 %s 전체 상품 동기화가 시작되었습니다.", marketType.name()),
        "status", "PROCESSING"
    ));
  }

  /**
   * 🚀 쿠팡 전용 역방향 동기화 트리거 API
   * 쿠팡의 모든 상품을 긁어와서 우리 DB의 MarketRegistration에 꽂아 넣습니다.
   * * [호출 URL] POST http://localhost:8080/api/admin/sync/COUPANG/reverse
   */
  @PostMapping("/{marketType}/reverse")
  public ResponseEntity<String> triggerReverseSync(@PathVariable MarketType marketType) {

    if (marketType != MarketType.COUPANG) {
      return ResponseEntity.badRequest().body("❌ 역방향 동기화는 현재 COUPANG 마켓만 지원합니다.");
    }

    log.info("API 호출 수신: {} 마켓 역방향 맵핑 대장정 시작", marketType);

    // 데이터가 많아 30~60분 이상 소요될 수 있으므로,
    // 메서드에 @Async를 걸어두고 HTTP 응답은 바로 내려보내는 것이 좋습니다!
    coupangReverseSyncProcessor.runReverseMapping();

    return ResponseEntity.ok("✅ 쿠팡 역방향 동기화 백그라운드 작업이 시작되었습니다. 서버 로그를 확인해주세요!");
  }
}