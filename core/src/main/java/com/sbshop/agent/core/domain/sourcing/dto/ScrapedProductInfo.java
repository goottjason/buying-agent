package com.sbshop.agent.core.domain.sourcing.dto;

import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ScrapedProductInfo {
    private final String sourceProductCode;
    private final String sourceUrl;
    
    // 기본 정보
    private final String nameEn;
    private final String brand;
    private final String originalCategory;
    
    // 가격 및 통화
    private final BigDecimal price;
    private final String currency;
    
    // 재고 및 상태
    private final StockStatus stockStatus;
    
    // 썸네일 및 상세 이미지
    private final String mainImageUrl;
    private final List<String> additionalImageUrls;
    
    // 크롤링 메타 데이터
    private final String rawSourceHtml;
    private final LocalDateTime scrapedAt;
}
