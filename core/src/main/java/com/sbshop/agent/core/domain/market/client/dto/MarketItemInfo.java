
package com.sbshop.agent.core.domain.market.client.dto;

import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import java.util.List;
// 공식적인 아키텍처 패턴 이름은 **'부패 방지 계층 (Anti-Corruption Layer, ACL) 객체'**
// MarketClient(Infra)가 더러운 JSON을 **"우리 도메인이 이해할 수 있는 깔끔하고 규격화된 DTO"**로 번역해서 던져주는 것
/**
 * 외부 마켓 API에서 조회한 비규격 데이터를 도메인 규칙에 맞게 정제한 Info 객체 (ACL 패턴)
 */
@Builder
public record MarketItemInfo(

    // =====================================================================
    // 1. 매핑 및 제어용 메타데이터
    // =====================================================================
    boolean isMasterData,                           // Product DB 덮어쓰기 허용 여부 (true면 DB 덮어쓰기, false면 스킵)
    String mappingKey,                              // 로컬 상품과 매칭하기 위한 핵심 식별키 (SKU or Cafe24 Code)
    Map<String, String> marketIdentifiers,          // 마켓별 고유 식별자 모음

    // =====================================================================
    // 2. 공통 마스터 상품 데이터 (Product 업데이트용)
    // =====================================================================
    String name,
    String originalName,
    BigDecimal salePrice,
    Integer stock,
    String detailHtml,
    List<String> images,
    String brand,
    String manufacturer,
    String categoryCode,
    String barcode,
    String generalProductName,

    // =====================================================================
    // 3. 마켓 전용 원본 데이터 (MarketRegistration 업데이트용)
    // =====================================================================
    Map<String, Object> rawData
) {
  /**
   * 정제된 마켓 데이터를 바탕으로 로컬 '상품(Product)' 업데이트 명령서를 생성합니다.
   */
  public ProductUpdateCommand toProductUpdateCommand() {
    return ProductUpdateCommand.builder()
        .name(this.name)
        .originalName(this.originalName)
        .salePrice(this.salePrice)
        .stock(this.stock)
        .detailHtml(this.detailHtml)
        .brand(this.brand)
        .manufacturer(this.manufacturer)
        // .categoryCode(this.categoryCode) // 필요 시 주석 해제
        .barcode(this.barcode)
        .baseName(this.generalProductName)
        .build();
  }
  /**
   * 정제된 마켓 데이터를 바탕으로 '연동 기록(MarketRegistration)' 업데이트 명령서를 생성합니다.
   */
  public MarketRegistrationUpdateCommand toRegistrationUpdateCommand() {
    return MarketRegistrationUpdateCommand.builder()
        .marketProductName(this.name())
        .marketIdentifiers(this.marketIdentifiers)
        .marketDetailedInfo(this.rawData)
        .isSynced(true)
        .lastSyncedAt(LocalDateTime.now())
        .build();
  }
}