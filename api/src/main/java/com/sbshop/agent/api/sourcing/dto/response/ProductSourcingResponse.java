package com.sbshop.agent.api.sourcing.dto.response;

import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import java.util.List;

/**
 * 프론트엔드(React)로 수집된 데이터를 전달하는 최종 응답 객체
 */
public record ProductSourcingResponse(
    String sourceUrl,
    int costPrice,
    String baseName,       // 핵심 한글 상품명
    String originalName,   // 원어 상품명 (영어)
    String brand,
    String origin,
    String weight,
    String expirationDate,
    int capacity,
    MeasureUnit measureUnit,
    List<String> sourceImages, // 메인/서브 통합 이미지 리스트
    String rawSourceHtml,      // 💡 나중에 HTML 굽기 위해 프론트가 보관할 원본 텍스트
    boolean isAvailable,
    String rawCategory,

    // 🚀 프론트엔드 가격 계산기에 세팅될 초기값들 전달!
    int bundleQuantity,
    int marginRate

) {

  // 🚀 ScrapedProductDto(순수 데이터)를 API Response 규격으로 변환하는 정적 팩토리 메서드
  public static ProductSourcingResponse from(ScrapedProductDto dto) {
    return new ProductSourcingResponse(
        dto.sourceUrl(),
        dto.costPrice(),
        dto.baseName(),
        dto.originalName(),
        dto.brand(),
        dto.origin(),
        dto.weight(),
        dto.expirationDate(),
        dto.capacity(),
        dto.measureUnit(),
        dto.sourceImages(),
        dto.rawSourceHtml(),
        dto.isAvailable(),
        dto.rawCategory(),
        // 💡 UseCase가 계산해준 데이터

        dto.bundleQuantity() != null ? dto.bundleQuantity() : 1,
        dto.marginRate() != null ? dto.marginRate() : 30
    );
  }
}