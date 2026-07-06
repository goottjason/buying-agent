package com.sbshop.agent.api.sourcing.scheduler;

import com.sbshop.agent.core.application.sourcing.service.SyncOrchestrator;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncOrchestrator syncOrchestrator;
    // private final ProductSourceRepository repository; // 생략 (Phase 5 데모용)

    /**
     * 매일 새벽 3시에 실행되는 일괄 동기화 배치 스케줄러
     */
    @Scheduled(cron = "0 0 3 * * ?") 
    public void scheduleDailyBulkSync() {
        log.info("[스케줄러] 매일 새벽 3시 일괄 상품 동기화 배치를 시작합니다.");
        
        try {
            // DB에서 "ACTIVE" 상태인 모든 상품을 페이징 혹은 Chunk 단위로 가져옴 (데모에선 더미 리스트 생성)
            // List<ProductSource> allSources = repository.findAllByStatus("ACTIVE");
            List<ProductSource> mockSources = List.of(); 
            
            syncOrchestrator.syncBulkProducts(mockSources);
            
            log.info("[스케줄러] 일괄 상품 동기화 배치 작업 완료.");
        } catch (Exception e) {
            log.error("[스케줄러] 일괄 상품 동기화 배치 작업 중 치명적 에러 발생: ", e);
        }
    }
}
