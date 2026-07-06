package com.sbshop.agent.core.application.sourcing.service;

import com.sbshop.agent.core.domain.sourcing.component.SourcingAgent;
import com.sbshop.agent.core.domain.sourcing.component.SourcingAgentFactory;
import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.sourcing.model.MarginPolicy;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import com.sbshop.agent.core.domain.sourcing.service.MarginPolicyEngine;
import com.sbshop.agent.core.domain.sourcing.service.ProductDiffDetector;
import com.sbshop.agent.core.domain.sourcing.service.SyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrchestrator {

    private final SourcingAgentFactory agentFactory;
    private final MarginPolicyEngine marginPolicyEngine;
    private final ProductDiffDetector diffDetector;
    private final SyncEventPublisher syncEventPublisher;
    
    // TODO: Repository DI

    /**
     * 관리자 화면(프론트엔드)에서 단일 상품의 수동 동기화를 요청할 때 실행되는 오케스트레이션 로직
     */
    @Transactional
    public void syncSingleProduct(String sourceUrl, ProductSource existingSource, MarginPolicy marginPolicy) {
        log.info("상품[{}] 수동 동기화를 시작합니다. (URL: {})", existingSource.getId(), sourceUrl);
        
        // 1. 해당 URL에 맞는 에이전트를 찾아 최신 가격/재고 스크래핑
        SourcingAgent agent = agentFactory.getAgentByUrl(sourceUrl);
        ScrapedProductInfo scrapedInfo = agent.scrapePriceAndStock(sourceUrl, existingSource.getSourceProductCode());
        
        // 2. 변경점(Diff) 감지
        ProductDiffDetector.DiffResult diff = diffDetector.detectChanges(existingSource, scrapedInfo);
        
        if (diff.isHasChanges()) {
            // 3. 변경이 있다면 새로운 최종 판매가 계산
            BigDecimal targetSalePrice = marginPolicyEngine.calculateTargetPrice(scrapedInfo.getPrice(), marginPolicy);
            
            // 4. 이벤트 발행을 통해 S3 업로드, 마켓 API 업데이트 등 후속 작업 지시
            syncEventPublisher.publishSyncEvent(existingSource.getId().toString(), diff, targetSalePrice);
            
            // 5. DB 상태 업데이트
            // existingSource.updateLastScrapedData(scrapedInfo.getPrice(), scrapedInfo.getStockStatus(), diff.getNewImageHash());
            // productSourceRepository.save(existingSource);
        } else {
            log.info("상품[{}] 변경사항 없음. 동기화 종료.", existingSource.getId());
        }
    }

    /**
     * 대용량 일괄 스케줄링 배치 처리를 위한 벌크 동기화 로직
     */
    @Transactional
    public void syncBulkProducts(List<ProductSource> sourcesToSync) {
        log.info("총 {}건의 상품 일괄 동기화(Batch) 작업을 시작합니다.", sourcesToSync.size());
        for (ProductSource source : sourcesToSync) {
            try {
                // TODO: MarginPolicy 조회
                MarginPolicy mockPolicy = MarginPolicy.builder()
                        .exchangeRate(BigDecimal.valueOf(1400))
                        .shippingFee(10000)
                        .marginRatePercent(30)
                        .commissionRatePercent(11)
                        .build();
                
                syncSingleProduct(source.getSourceUrl(), source, mockPolicy);
                
                // 마켓 API의 Rate Limit 등을 고려한 미세 휴식 (실제로는 Queue에서 처리 권장)
                Thread.sleep(100); 
            } catch (Exception e) {
                log.error("상품[{}] 동기화 중 에러 발생: {}", source.getId(), e.getMessage());
            }
        }
    }
}
