
package com.sbshop.agent.core.domain.product.port.dto;

import java.util.Map;
import lombok.Builder;
import java.util.List;

@Builder
public record MarketExtractedData(
    // 나중에 명확하게 매핑할 공통 필드들 (일단 유지)
    String detailHtml,
    List<String> images,

    // 2. 마켓별 고유 세부 데이터 (유연한 바구니)
    Map<String, Object> rawData
) {}