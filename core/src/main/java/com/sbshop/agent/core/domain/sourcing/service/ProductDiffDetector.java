package com.sbshop.agent.core.domain.sourcing.service;

import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class ProductDiffDetector {

    /**
     * 기존 DB 데이터(ProductSource)와 새롭게 스크래핑된 데이터(ScrapedProductInfo)를 비교하여 변경점을 감지합니다.
     */
    public DiffResult detectChanges(ProductSource existingSource, ScrapedProductInfo scrapedInfo) {
        boolean priceChanged = existingSource.getLastScrapedPrice() == null || 
                               existingSource.getLastScrapedPrice().compareTo(scrapedInfo.getPrice()) != 0;
        
        boolean stockChanged = existingSource.getLastScrapedStockStatus() != scrapedInfo.getStockStatus();
        
        // 썸네일 해시는 URL 자체로 비교하거나, 필요 시 실제 파일 해시를 비교 (Phase 4 에서는 URL 기준으로 약식 판별)
        String newImageHash = generateImageHash(scrapedInfo.getMainImageUrl());
        boolean imageChanged = !Objects.equals(existingSource.getLastScrapedImageHash(), newImageHash);

        return DiffResult.builder()
                .hasChanges(priceChanged || stockChanged || imageChanged)
                .priceChanged(priceChanged)
                .stockChanged(stockChanged)
                .imageChanged(imageChanged)
                .oldPrice(existingSource.getLastScrapedPrice())
                .newPrice(scrapedInfo.getPrice())
                .oldStockStatus(existingSource.getLastScrapedStockStatus())
                .newStockStatus(scrapedInfo.getStockStatus())
                .newImageHash(newImageHash)
                .build();
    }

    private String generateImageHash(String url) {
        // 실제 운영 환경에서는 이미지를 다운로드하여 MD5/SHA-256 해시를 생성해야 하지만,
        // 여기서는 Phase 4 데모를 위해 URL 해시코드로 대체합니다.
        return url == null ? null : String.valueOf(url.hashCode());
    }

    @Getter
    @Builder
    public static class DiffResult {
        private final boolean hasChanges;
        private final boolean priceChanged;
        private final boolean stockChanged;
        private final boolean imageChanged;
        
        private final BigDecimal oldPrice;
        private final BigDecimal newPrice;
        private final StockStatus oldStockStatus;
        private final StockStatus newStockStatus;
        
        private final String newImageHash;
    }
}
