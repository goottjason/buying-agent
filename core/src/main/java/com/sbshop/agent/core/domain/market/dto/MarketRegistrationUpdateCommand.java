package com.sbshop.agent.core.domain.market.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import java.util.Map;

@Builder
public record MarketRegistrationUpdateCommand(
    // 마켓별 고유 식별자 모음 (예: product_no, originProductNo 등)
    Map<String, String> marketIdentifiers,

    // 마켓 원본 데이터 (JSON)
    Map<String, Object> marketDetailedInfo,
    Boolean isSynced,
    LocalDateTime lastSyncedAt

) {}