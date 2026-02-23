package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductAppender;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.enums.VendorType;
import com.sbshop.agent.core.domain.product.vo.ImageInfo;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.vo.SourcingInfo;
import com.sbshop.agent.infrastructure.csv.ProductCsvDto;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMigrationProcessor {

  private final ProductAppender productAppender;

  @Transactional
  public int migrateFromCsv(List<ProductCsvDto> csvList) {
    int successCount = 0;

    for (ProductCsvDto dto : csvList) {
      try {
        // 1. VO 조립
        ProductSpec productSpec = ProductSpec.builder()
            .barcode(dto.getBarcode())
            .capacity(parseDecimal(dto.getCapacity()))
            .measureUnit(parseMeasureUnit(dto.getMeasureUnit()))
            .build();

        SourcingInfo sourcingInfo = SourcingInfo.builder()
            .vendor(parseVendor(dto.getVendor()))
            .sourceUrl(dto.getSourceUrl())
            .manufacturer(dto.getManufacturer())
            .origin(dto.getOrigin())
            .hsCode(dto.getHsCode())
            .build();

        PriceInfo priceInfo = PriceInfo.builder()
            .costPrice(parseDecimal(dto.getCostPrice()))
            .exchangeRate(parseDecimal(dto.getExchangeRate()))
            .deliveryFee(parseDecimal(dto.getDeliveryFee()))
            .marginRate(parseDecimal(dto.getMarginRate()))
            .salePrice(parseDecimal(dto.getSalePrice()))
            .build();

        LogisticsInfo logisticsInfo = LogisticsInfo.builder()
            .stock(parseInt(dto.getStock()))
            .weight(parseDecimal(dto.getWeight()))
            .bundleQuantity(parseInt(dto.getBundleQuantity()))
            .build();
        ImageInfo imageInfo = ImageInfo.builder()
            .sourceImages(parseImageList(dto.getSourceImages()))
            .hostedImages(parseImageList(dto.getHostedImages()))
            .build();

        // 2. 메인 엔티티 조립
        Product product = Product.builder()
            .sku(dto.getSku())
            .brand(dto.getBrand())
            .name(dto.getName())
            .originalName(dto.getOriginalName())
            .category(parseCategory(dto.getCategory()))
            .productSpec(productSpec)
            .sourcingInfo(sourcingInfo)
            .priceInfo(priceInfo)
            .logisticsInfo(logisticsInfo)
            .imageInfo(imageInfo)
            .searchKeywords(dto.getSearchKeywords())
            .detailHtml(dto.getDetailHtml())
            .memo(dto.getMemo())
            .build();

        // 4. DB에 저장!
        productAppender.append(product);
        successCount++;

      } catch (Exception e) {
        // 특정 상품 1개가 에러 나더라도 전체가 멈추지 않게 로그만 남기고 다음 상품으로 넘어갑니다.
        log.error("상품 마이그레이션 실패 (SKU: {}): {}", dto.getSku(), e.getMessage());
      }
    }
    return successCount;
  }

  // ==========================================
  // 아래는 파싱을 돕는 안전한 변환 유틸리티 메서드들입니다.
  // ==========================================

  // 문자를 Enum(VendorType)으로 변환 (오타나 빈 값이면 UNKNOWN 처리)
  private VendorType parseVendor(String vendorStr) {
    if (vendorStr == null || vendorStr.isBlank()) return VendorType.UNKNOWN;
    try {
      return VendorType.valueOf(vendorStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("등록되지 않은 Vendor 코드입니다: {}", vendorStr);
      return VendorType.UNKNOWN;
    }
  }

  // 문자를 Enum(CategoryType)으로 변환 (매칭 안되면 null 처리)
  private CategoryType parseCategory(String categoryStr) {
    if (categoryStr == null || categoryStr.isBlank()) return null;
    try {
      return CategoryType.valueOf(categoryStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  // 문자를 소수점 숫자(BigDecimal)로 변환 (빈 값이면 0.0 처리)
  private BigDecimal parseDecimal(String val) {
    if (val == null || val.isBlank()) return BigDecimal.ZERO;
    try {
      // 혹시라도 엑셀에 콤마가 섞여 들어왔을 경우를 대비한 방어 코드입니다.
      return new BigDecimal(val.replaceAll(",", "").trim());
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  // 문자를 정수(Integer)로 변환 (빈 값이면 0 처리)
  private Integer parseInt(String val) {
    if (val == null || val.isBlank()) return 0;
    try {
      double d = Double.parseDouble(val.replaceAll(",", "").trim());
      return (int) d;
    } catch (Exception e) {
      return 0;
    }
  }
  private List<String> parseImageList(String imageStr) {
    List<String> result = new ArrayList<>();
    if (imageStr == null || imageStr.isBlank()) {
      return result;
    }

    String[] urls = imageStr.split(",");
    for (String url : urls) {
      String trimmed = url.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }
  private MeasureUnit parseMeasureUnit(String unitStr) {
    if (unitStr == null || unitStr.isBlank()) return MeasureUnit.UNKNOWN;
    try {
      return MeasureUnit.valueOf(unitStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      return MeasureUnit.UNKNOWN;
    }
  }
}