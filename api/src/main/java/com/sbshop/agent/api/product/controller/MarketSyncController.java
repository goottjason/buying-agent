package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.processor.MarketBatchSyncProcessor;
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

  private final ProductSyncProcessor productSyncProcessor;
  private final MarketBatchSyncProcessor batchSyncProcessor;
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
    batchSyncProcessor.syncAllAsync(marketType);

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", String.format("백그라운드에서 %s 전체 상품 동기화가 시작되었습니다.", marketType.name()),
        "status", "PROCESSING"
    ));
  }
}