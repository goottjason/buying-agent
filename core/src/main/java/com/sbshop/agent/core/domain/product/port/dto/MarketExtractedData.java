
package com.sbshop.agent.core.domain.product.port.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Builder;
import java.util.List;

@Builder
public record MarketExtractedData(
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