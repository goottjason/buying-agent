
package com.sbshop.agent.core.domain.product.port.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Builder;
import java.util.List;

@Builder
public record MarketExtractedData(
    // 🚀 [추가] 마스터 데이터 여부 스위치! (true면 DB 덮어쓰기, false면 스킵)
    boolean isMasterData,

    // 🚀 [추가] 마켓별 고유 식별자 모음 (개수가 몇 개든 다 담을 수 있는 유연한 바구니!)
    Map<String, String> marketIdentifiers,

    // 1. 마스터 DB 업데이트용 핵심 데이터
    String name,
    String originalName,
    BigDecimal salePrice,
    Integer stock,
    String detailHtml,
    List<String> images,

    // 2. 마켓별 고유 세부 데이터 (유연한 바구니)
    Map<String, Object> rawData
) {}