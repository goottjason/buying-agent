package com.sbshop.agent.core.domain.product.dto;

import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 부분 업데이트(Partial Update)를 완벽하게 지원하는 평탄화된 Command
 * 변경을 원하는 필드에만 값을 넣고, 변경하지 않을 필드는 null로 둡니다.
 */
@Builder
public record ProductUpdateCommand(

    // 1. 기본 Flat 필드
    String brand,
    String name,
    String baseName,
    String originalName,
    CategoryType category,
    String searchKeywords,
    String detailHtml,
    String memo,

    // 2. PriceInfo 관련 필드 (펼침!)
    BigDecimal costPrice,
    BigDecimal exchangeRate,
    BigDecimal deliveryFee,
    BigDecimal marginRate,
    BigDecimal salePrice,

    // 3. LogisticsInfo 관련 필드 (펼침!)
    Integer stock,
    BigDecimal weight,
    Integer bundleQuantity,

    // 4. ImageInfo 관련 필드 (펼침!)
    List<String> sourceImages,
    List<String> hostedImages,

    // 5. ProductSpec 관련 필드 (펼침!)
    String barcode,
    BigDecimal capacity,
    MeasureUnit measureUnit,

    // 6. SourcingInfo 관련 필드 (펼침!)
    VendorType vendor,
    String sourceUrl,
    String manufacturer,
    String origin,
    String hsCode
) {}