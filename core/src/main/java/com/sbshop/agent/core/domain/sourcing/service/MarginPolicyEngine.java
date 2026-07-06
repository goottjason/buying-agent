package com.sbshop.agent.core.domain.sourcing.service;

import com.sbshop.agent.core.domain.sourcing.model.MarginPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MarginPolicyEngine {

    /**
     * 최종 마켓 판매가 계산
     * 공식: (소싱 원가 * 환율 + 기본 배송비) * (1 + 마진율) * (1 + 마켓 수수료율)
     */
    public BigDecimal calculateTargetPrice(BigDecimal sourceCostPrice, MarginPolicy policy) {
        if (sourceCostPrice == null || policy == null) {
            return BigDecimal.ZERO;
        }

        // 1. 원화 환산 가격 = 원가 * 환율
        BigDecimal priceInKrw = sourceCostPrice.multiply(policy.getExchangeRate());
        
        // 2. 배송비 포함 원가 = 원화 환산 가격 + 배송비
        BigDecimal baseCost = priceInKrw.add(BigDecimal.valueOf(policy.getShippingFee()));
        
        // 3. 마진 적용가 = 배송비 포함 원가 * (1 + 마진율/100)
        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                BigDecimal.valueOf(policy.getMarginRatePercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        BigDecimal priceWithMargin = baseCost.multiply(marginMultiplier);
        
        // 4. 마켓 수수료 적용 (최종가) = 마진 적용가 * (1 + 수수료율/100)
        BigDecimal commissionMultiplier = BigDecimal.ONE.add(
                BigDecimal.valueOf(policy.getCommissionRatePercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        BigDecimal finalPrice = priceWithMargin.multiply(commissionMultiplier);
        
        // 백원 단위 반올림 처리 (예: 12,345 -> 12,300)
        return finalPrice.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
