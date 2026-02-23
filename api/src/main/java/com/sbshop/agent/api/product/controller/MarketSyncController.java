package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.processor.MarketBatchSyncProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
   * 임시 API: 카페24 전체 동기화 트리거
   * POST http://localhost:8080/api/admin/sync/cafe24/all
   */
  @PostMapping("/cafe24/all")
  public ResponseEntity<?> syncAllCafe24() {
    log.info("API 호출 수신: 카페24 전체 동기화 트리거 작동");

    // 백그라운드 작업을 호출만 하고 바로 다음 줄로 넘어갑니다. (비동기 마법)
    batchSyncProcessor.syncAllWithCafe24Async();

    // 사용자(또는 프론트엔드)에게는 즉시 200 OK 응답을 줍니다.
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "백그라운드에서 카페24 전체 상품 동기화가 시작되었습니다. 로그를 확인해주세요.",
        "status", "PROCESSING"
    ));
  }
}