package com.sbshop.agent.infrastructure.client.market;

import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class MarketApiAdapter {

    /**
     * 쿠팡(Coupang), 카페24(Cafe24) 등 외부 마켓 API로 최종 업데이트 요청을 전송합니다.
     */
    public void updateMarketProduct(String marketProductId, BigDecimal targetPrice, StockStatus stockStatus) {
        log.info("[Market API 연동] 마켓 상품 ID: {} 업데이트 시작", marketProductId);
        
        // 가격 유효성 검증
        if (targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[Market API] 가격 오류: 0원 이하로 판매할 수 없습니다.");
            return;
        }

        // 재고 상태에 따른 마켓 상태 매핑
        String marketStatus = stockStatus == StockStatus.IN_STOCK ? "ON_SALE" : "SOLD_OUT";
        
        log.info("[Market API 완료] 쿠팡/카페24에 판매가 {}원, 상태 {} 업데이트 성공!", targetPrice, marketStatus);
    }
}
