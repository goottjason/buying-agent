
package com.sbshop.agent.core.domain.product.port.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record MarketProductDto(
    String detailHtml,
    List<String> images
) {}