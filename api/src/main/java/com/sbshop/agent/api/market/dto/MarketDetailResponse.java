package com.sbshop.agent.api.market.dto;

import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

/**
 * 프론트엔드의 팝업 모달창에 뿌려질 마켓 상세 데이터 응답 객체
 */
@Builder
public record MarketDetailResponse(
    String marketName,      // 마켓에 등록된 실제 상품명
    BigDecimal salePrice,   // 마켓 판매가
    Integer stock,          // 마켓 재고
    String brand,           // 브랜드
    String mappingKey,      // 매핑에 사용된 기준 키 (상품코드 등)
    List<String> images     // 마켓 썸네일 이미지들
) {
  // 🚀 Core의 Info 객체를 받아 화면용으로 매핑
  public static MarketDetailResponse from(MarketItemInfo info) {
    return MarketDetailResponse.builder()
        .marketName(info.name())
        .salePrice(info.salePrice())
        .stock(info.stock())
        .brand(info.brand())
        .mappingKey(info.mappingKey())
        .images(info.images())
        .build();
  }
}