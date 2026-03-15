package com.sbshop.agent.core.application.sourcing.client;

import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;

/**
 * 아이허브 크롤링을 전담하는 외부 통신 클라이언트
 */
public interface ScraperClient {
  // 1. URL을 주면 -> 2. 데이터를 긁어서 -> 3. RawProductData로 반환!
  ScrapedProductDto scrape(String url);
}