package com.sbshop.agent.core.domain.product.dto;

import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Builder(toBuilder = true) // 💡 UseCase에서 기존 값을 유지한 채 필드를 추가(Enrich)하기 위해 사용
public record ProductCreateCommand(
    // 1. 기본 정보
    String brand,
    String baseName,
    String originalName,
    String rawCategory,     // 원본 카테고리 문자열
    CategoryType category,  // 💡 UseCase에서 매핑할 Enum 카테고리

    // 2. 가격 및 물류
    BigDecimal costPrice,
    BigDecimal marginRate,
    Integer bundleQuantity,
    BigDecimal weight,
    boolean isAvailable,    // 재고 상태 계산용

    // 3. 스펙 및 출처
    BigDecimal capacity,
    MeasureUnit measureUnit,
    VendorType vendor,
    String sourceUrl,
    String origin,
    String expirationDate,
    String hsCode,          // 💡 UseCase에서 추가할 세관 코드

    // 4. 이미지 및 HTML 재료
    List<String> sourceImages,
    List<String> hostedImages, // 💡 UseCase에서 R2 업로드 후 추가
    String rawSourceHtml       // 💡 엔티티가 HTML을 구울 때 쓰고 버릴 1회용 재료
) {}