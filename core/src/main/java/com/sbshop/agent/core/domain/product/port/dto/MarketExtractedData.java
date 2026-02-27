
package com.sbshop.agent.core.domain.product.port.dto;

import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import java.util.List;

@Builder
public record MarketExtractedData(
    // 🚀 [추가] 마스터 데이터 여부 스위치! (true면 DB 덮어쓰기, false면 스킵)
    boolean isMasterData,
    // 🚀 [추가] 마켓별 고유 식별자 모음 (개수가 몇 개든 다 담을 수 있는 유연한 바구니!)
    Map<String, String> marketIdentifiers,
    // 🚀 [신규] 우리 DB와 매칭할 때 쓸 공용 마스터 열쇠 (SKU or Cafe24 Code)
    String mappingKey,

    // 1. 마스터 DB 업데이트용 핵심 데이터
    String name,
    String originalName,
    BigDecimal salePrice,
    Integer stock,
    String detailHtml,
    List<String> images,

    // 🚀 2. [추가] 고부가가치 마스터 데이터 (쿠팡 등에서 추출)
    String brand,
    String manufacturer,
    String categoryCode,
    String barcode,
    String generalProductName,

    // 2. 마켓별 고유 세부 데이터 (유연한 바구니)
    Map<String, Object> rawData
) {
  // 🚀 1. 상품(Product) 업데이트 커맨드로 변환
  public ProductUpdateCommand toProductUpdateCommand() {
    return ProductUpdateCommand.builder()
        .name(this.name)
        .originalName(this.originalName)
        .salePrice(this.salePrice)
        .stock(this.stock)
        .detailHtml(this.detailHtml)
        .brand(this.brand)
        .manufacturer(this.manufacturer)
        // .categoryCode(this.categoryCode)
        .barcode(this.barcode)
        .baseName(this.generalProductName)
        .build();
  }
  // 🚀 2. 마켓 등록 정보(MarketRegistration) 업데이트 커맨드로 변환
  // (진짜 SKU와 마켓 타입을 외부에서 주입받습니다!)
  public MarketRegistrationUpdateCommand toRegistrationUpdateCommand() {
    return MarketRegistrationUpdateCommand.builder()
        // 1. 어댑터가 물어온 마켓별 고유 식별자 모음
        .marketIdentifiers(this.marketIdentifiers)

        // 2. 마켓 원본 데이터 (JSON)
        .marketDetailedInfo(this.rawData)

        // 🚀 3. 동기화 성공 여부 (정상적으로 추출되었으므로 true)
        .isSynced(true)

        // 🚀 4. 마지막 동기화 시간 (현재 시간으로 갱신!)
        .lastSyncedAt(LocalDateTime.now())

        .build();
  }
}