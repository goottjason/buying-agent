package com.sbshop.agent.core.domain.sourcing.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SourcingSiteCode {
    IHERB("아이허브", "USD"),
    AMAZON_US("아마존(US)", "USD"),
    AMAZON_UK("아마존(UK)", "GBP"),
    OCADO("오카도", "GBP"),
    TESCO("테스코", "GBP");

    private final String description;
    private final String defaultCurrency;
}
