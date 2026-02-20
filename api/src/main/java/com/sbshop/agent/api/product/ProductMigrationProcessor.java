package com.sbshop.agent.api.product;

import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductAppender;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.enums.VendorType;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
import com.sbshop.agent.infrastructure.csv.ProductCsvDto;
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
        // 1. VO 조립하기 (가격 정보)
        // String으로 들어온 값을 안전하게 BigDecimal로 변환하여 Builder에 넣습니다.
        PriceInfo priceInfo = PriceInfo.builder()
            .costPrice(parseDecimal(dto.getCostPrice()))
            .exchangeRate(parseDecimal(dto.getExchangeRate()))
            .deliveryFee(parseDecimal(dto.getDeliveryFee()))
            .marginRate(parseDecimal(dto.getMarginRate()))
            .salePrice(parseDecimal(dto.getSalePrice()))
            .build();

        // 2. VO 조립하기 (물류 정보)
        LogisticsInfo logisticsInfo = LogisticsInfo.builder()
            .stock(parseInt(dto.getStock()))
            .weight(parseDecimal(dto.getWeight()))
            .bundleQuantity(parseInt(dto.getBundleQuantity()))
            .build();

        // 3. 엔티티 조립하기 (Product)
        Product product = Product.builder()
            .sku(dto.getSku())
            .name(dto.getName())
            .originalName(dto.getOriginalName())
            .category(parseCategory(dto.getCategory())) // String -> Enum 변환
            .vendor(parseVendor(dto.getVendor()))       // String -> Enum 변환
            .sourceUrl(dto.getSourceUrl())
            .priceInfo(priceInfo)         // 만들어둔 VO를 쏙 넣습니다.
            .logisticsInfo(logisticsInfo) // 만들어둔 VO를 쏙 넣습니다.
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
}