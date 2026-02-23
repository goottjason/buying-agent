package com.sbshop.agent.core.domain.product.port;

import lombok.Builder;
import java.util.List;

@Builder
public record Cafe24ProductDto(
    String detailHtml,
    List<String> images
) {}