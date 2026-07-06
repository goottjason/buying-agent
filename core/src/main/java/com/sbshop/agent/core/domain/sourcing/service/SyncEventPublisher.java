package com.sbshop.agent.core.domain.sourcing.service;

import com.sbshop.agent.core.domain.sourcing.service.ProductDiffDetector.DiffResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class SyncEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SyncEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 감지된 변경점(DiffResult)을 기반으로 적절한 동기화 이벤트(Event)를 발행합니다.
     */
    public void publishSyncEvent(String productId, DiffResult diff, BigDecimal targetSalePrice) {
        if (!diff.isHasChanges()) {
            log.debug("상품[{}] 변경점 없음. 동기화 스킵.", productId);
            return;
        }

        if (diff.isPriceChanged() || diff.isStockChanged()) {
            log.info("상품[{}] 가격/재고 변경 감지! (새 판매가: {}, 새 상태: {}). 이벤트 발행 중...", 
                    productId, targetSalePrice, diff.getNewStockStatus());
            // TODO: 실제 이벤트 객체 발행
            // eventPublisher.publishEvent(new ProductPriceStockChangedEvent(productId, targetSalePrice, diff.getNewStockStatus()));
        }

        if (diff.isImageChanged()) {
            log.info("상품[{}] 썸네일 해시 변경 감지. 이미지 재생성 및 템플릿 재구성 이벤트 발행 중...", productId);
            // TODO: 실제 이벤트 객체 발행
            // eventPublisher.publishEvent(new ProductImageChangedEvent(productId, diff.getNewImageHash()));
        }
    }
}
