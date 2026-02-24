package com.sbshop.agent.core.domain.product.dto;

import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.vo.ImageInfo;
import com.sbshop.agent.core.domain.product.model.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.model.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.model.vo.SourcingInfo;
import lombok.Builder;

@Builder
public record ProductUpdateCommand(
    String brand,
    String name,
    String originalName,
    CategoryType category,
    ProductSpec productSpec,
    SourcingInfo sourcingInfo,
    PriceInfo priceInfo,
    LogisticsInfo logisticsInfo,
    ImageInfo imageInfo,
    String searchKeywords,
    String detailHtml,
    String memo
) {}