package com.sbshop.agent.core.domain.product.dto;

import lombok.Builder;

@Builder
public record ProductUpdateCommand(
    String name,
    String detailHtml,
    String memo
    // 필요한 필드들을 여기에 계속 추가하시면 됩니다.
) {}