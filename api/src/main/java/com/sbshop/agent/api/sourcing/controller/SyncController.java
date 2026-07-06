package com.sbshop.agent.api.sourcing.controller;

import com.sbshop.agent.core.application.sourcing.service.SyncOrchestrator;
import com.sbshop.agent.core.domain.sourcing.model.MarginPolicy;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncOrchestrator syncOrchestrator;
    
    /**
     * 관리자 대시보드(Frontend)에서 특정 상품을 수동으로 동기화 요청하는 API
     */
    @PostMapping("/single/{productSourceId}")
    public ResponseEntity<String> triggerSingleSync(@PathVariable Long productSourceId) {
        log.info("프론트엔드 대시보드로부터 상품[{}] 수동 동기화 요청 접수.", productSourceId);
        
        // TODO: DB에서 id로 ProductSource 및 MarginPolicy 조회
        // 여기서는 Phase 5 연동 테스트를 위해 Mock 객체 활용
        ProductSource mockSource = ProductSource.builder().sourceUrl("https://www.iherb.com/pr/mock/1234").build();
        MarginPolicy mockPolicy = MarginPolicy.builder().build();
        
        syncOrchestrator.syncSingleProduct(mockSource.getSourceUrl(), mockSource, mockPolicy);
        
        return ResponseEntity.ok("상품 " + productSourceId + " 동기화 트리거 완료.");
    }

    /**
     * 관리자 대시보드(Frontend)에서 일괄 동기화(Batch)를 요청하는 API
     */
    @PostMapping("/bulk")
    public ResponseEntity<String> triggerBulkSync(@RequestBody List<Long> productSourceIds) {
        log.info("프론트엔드 대시보드로부터 상품 {}건 일괄 동기화 요청 접수.", productSourceIds.size());
        
        // TODO: DB에서 List<ProductSource> 조회
        List<ProductSource> mockSources = List.of(); 
        
        // 비동기 처리 권장
        syncOrchestrator.syncBulkProducts(mockSources);
        
        return ResponseEntity.ok("일괄 동기화 작업이 백그라운드 큐에 등록되었습니다.");
    }
}
