package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.processor.MarketBatchSyncProcessor;
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

  private final MarketBatchSyncProcessor batchSyncProcessor;

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