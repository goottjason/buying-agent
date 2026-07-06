package com.sbshop.agent.core.domain.sourcing.component;

import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;

import java.util.List;

public interface SourcingAgent {
    
    /** 
     * 해당 에이전트가 처리할 수 있는 소싱처 코드 반환 
     */
    SourcingSiteCode getSiteCode();

    /** 
     * 단일 상품 스크래핑 
     */
    ScrapedProductInfo scrapeProduct(String sourceUrl, String sourceProductCode);

    /** 
     * 다중 상품 병렬 스크래핑 (API Limit 고려) 
     */
    List<ScrapedProductInfo> scrapeProducts(List<String> sourceUrls);
    
    /** 
     * 최신 가격 및 재고 상태만 빠르게 스크래핑 (배치 스케줄링 용)
     */
    ScrapedProductInfo scrapePriceAndStock(String sourceUrl, String sourceProductCode);
}
