package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import com.sbshop.agent.core.domain.product.model.Product;
import java.math.BigDecimal;

public record ProductDetailResponse(
    Long id,
    String sku,
    String brand,
    String name,
    String baseName,
    String originalName,
    String category,
    String searchKeywords,
    String detailHtml,

    // 💡 프론트엔드가 요구하는 계층형 구조
    SourcingInfoDto sourcingInfo,
    LogisticsInfoDto logisticsInfo,
    ProductSpecDto productSpec,
    PriceInfoDto priceInfo
) {

  // 🚀 Aggregate(엔티티) -> DTO 변환 팩토리 메서드
  public static ProductDetailResponse from(ProductMarketAggregate aggregate) {
    Product p = aggregate.product();

    return new ProductDetailResponse(
        p.getId(),
        p.getSku(),
        p.getBrand(),
        p.getName(),
        p.getBaseName(),
        p.getOriginalName(),
        p.getCategory() != null ? p.getCategory().getTitle() : "미분류",
        p.getSearchKeywords(),
        p.getDetailHtml(),

        // 💡 하위 객체들 널 세이프(Null-Safe) 매핑
        new SourcingInfoDto(
            p.getSourcingInfo() != null ? p.getSourcingInfo().getVendor().name() : "-",
            p.getSourcingInfo() != null ? p.getSourcingInfo().getOrigin() : "-",
            p.getSourcingInfo() != null ? p.getSourcingInfo().getHsCode() : "-"
        ),
        new LogisticsInfoDto(
            p.getLogisticsInfo() != null ? p.getLogisticsInfo().getWeight() : BigDecimal.ZERO,
            p.getLogisticsInfo() != null ? p.getLogisticsInfo().getBundleQuantity() : 1
        ),
        new ProductSpecDto(
            p.getProductSpec() != null ? p.getProductSpec().getCapacity() : BigDecimal.ZERO,
            p.getProductSpec() != null ? p.getProductSpec().getMeasureUnit().name() : "-"
        ),
        new PriceInfoDto(
            p.getPriceInfo() != null ? p.getPriceInfo().getCostPrice() : BigDecimal.ZERO,
            p.getPriceInfo() != null ? p.getPriceInfo().getMarginRate() : BigDecimal.ZERO,
            p.getPriceInfo() != null ? p.getPriceInfo().getSalePrice() : BigDecimal.ZERO
        )
    );
  }

  // =================================================================
  // 🛠️ 내부 레코드 (프론트엔드와 1:1 매칭용)
  // =================================================================
  public record SourcingInfoDto(String vendor, String origin, String hsCode) {}
  public record LogisticsInfoDto(BigDecimal weight, int bundleQuantity) {}
  public record ProductSpecDto(BigDecimal capacity, String measureUnit) {}
  public record PriceInfoDto(BigDecimal costPrice, BigDecimal marginRate, BigDecimal salePrice) {}
}