package com.sbshop.agent.core.application.product.dto;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import lombok.Builder;

/**
 * 화면(UI) 구조는 전혀 모르는 순수 도메인 객체들의 묶음
 */
@Builder
public record ProductMarketAggregate(
    Product product,
    List<MarketRegistration> registrations
) {
}