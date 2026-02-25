package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;

public interface MarketDataExtractorPort extends MarketBasePort {
  // 2단계: 마켓 고유 번호로 무거운 파싱(HTML, 이미지 등)을 거쳐 뚱뚱한 DTO 반환하기
  MarketExtractedData extractInitialProductData(String marketProductNo);
}
