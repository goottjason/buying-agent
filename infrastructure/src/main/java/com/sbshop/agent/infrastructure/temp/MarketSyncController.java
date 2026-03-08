/*
package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.core.application.product.ProductSyncUseCase;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class MarketSyncController {

  private final ProductSyncUseCase productSyncUseCase;

  @PostMapping("/{marketType}/perfect")
  public ResponseEntity<String> triggerPerfectSync(@PathVariable MarketType marketType) {
    log.info("API 호출 수신: {} 마켓 완벽 동기화 시작", marketType);
    productSyncUseCase.runPerfectSync(marketType);
    return ResponseEntity.ok("✅ " + marketType + " 마켓 완벽 동기화 백그라운드 작업이 시작되었습니다.");
  }
}*/
