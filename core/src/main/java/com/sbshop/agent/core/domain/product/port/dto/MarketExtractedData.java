
package com.sbshop.agent.core.domain.product.port.dto;

import java.math.BigDecimal;
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
) {}