package com.sbshop.agent.core.application.sourcing.dto;

import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import lombok.Builder;
import java.util.List;

/**
 * 아이허브 파서가 긁어온 순수 데이터를 담는 배달통 (비즈니스 로직 없음)
 */
@Builder(toBuilder = true) // 💡 UseCase에서 데이터를 덧붙일 수 있도록 toBuilder 활성화!
public record ScrapedProductDto(
    String sourceUrl,
    int costPrice,
    String baseName,
    String originalName,
    String brand,
    String origin,
    String weight,
    String expirationDate,
    int capacity,
    MeasureUnit measureUnit,
    List<String> sourceImages, // 메인/서브 통합
    String rawSourceHtml,      // 원본 HTML
    boolean isAvailable,
    String rawCategory,

    // 🚀 UseCase가 계산해서 채워줄 비즈니스 정책 필드 (프론트엔드 초기값용)
    Integer bundleQuantity,
    Integer marginRate
) {}