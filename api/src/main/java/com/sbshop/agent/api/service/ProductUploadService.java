package com.sbshop.agent.api.service;

import com.sbshop.agent.core.domain.product.MarketRegistration;
import com.sbshop.agent.core.domain.product.MarketRegistrationRepository;
import com.sbshop.agent.core.domain.product.MarketType;
import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductRepository;
import com.sbshop.agent.infrastructure.csv.ProductCsvDto; // 풀 패키지명
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUploadService {

  private final ProductRepository productRepository;
  private final MarketRegistrationRepository marketRegistrationRepository;

  @Transactional
  public int saveProductsFromCsv(List<ProductCsvDto> csvList) {
    int successCount = 0;

    for (ProductCsvDto dto : csvList) {
      try {
        // 1. [Product] 상품 메인 정보 생성 및 저장
        Product product = Product.builder()
            .sbCode(dto.getSbCode())
            .korName(dto.getKorName())
            .engName(dto.getEngName())
            .categoryName(dto.getCategory())
            .sourcingMkt(dto.getSourcingMkt()) // CSV: mkt
            .sourceUrl(dto.getSourceUrl())
            .costPrice(parseDecimal(dto.getCostPrice()))
            .exchangeRate(parseDecimal(dto.getExchangeRate()))
            .shippingPrice(parseDecimal(dto.getShippingPrice()))
            .marginRate(parseDecimal(dto.getMarginRate()))
            .finalSalePrice(parseDecimal(dto.getFinalSalePrice()))
            .stockQuantity(parseInt(dto.getStockQuantity()))
            .weight(parseDecimal(dto.getWeight()))
            .packageInfo(dto.getPackageInfo())
            .packageQuantity(parseInt(dto.getPackageQuantity())) // CSV: packQty
            .htmlContent(dto.getHtmlContent())
            .memo(dto.getMemo())
            .build();

        // ★ 상품을 먼저 저장해야 ID가 생성됨 (영속화)
        Product savedProduct = productRepository.save(product);

        // 2. [MarketRegistration] 마켓별 연동 정보 생성 및 저장

        // [쿠팡]
        if (hasText(dto.getVendorItemId()) || hasText(dto.getCoupOptCode())) {
          Map<String, Object> ids = new HashMap<>();
          ids.put("vendorItemId", dto.getVendorItemId());
          ids.put("coupOptCode", dto.getCoupOptCode());
          ids.put("sellerProductId", dto.getSellerProductId());

          saveMarketRegistration(savedProduct, MarketType.COUPANG, ids);
        }

        // [네이버 스마트스토어]
        if (hasText(dto.getNavCode())) {
          Map<String, Object> ids = new HashMap<>();
          ids.put("navCode", dto.getNavCode());

          saveMarketRegistration(savedProduct, MarketType.SMART_STORE, ids);
        }

        // [지마켓/옥션 (ESM)]
        if (hasText(dto.getGmktCode()) || hasText(dto.getActCode())) {
          Map<String, Object> ids = new HashMap<>();
          ids.put("gmktCode", dto.getGmktCode());
          ids.put("actCode", dto.getActCode());

          saveMarketRegistration(savedProduct, MarketType.ESM, ids);
        }

        // [카페24 (자사몰)]
        if (hasText(dto.getCafeNo()) || hasText(dto.getCafeCode())) {
          Map<String, Object> ids = new HashMap<>();
          ids.put("cafeNo", dto.getCafeNo());
          ids.put("cafeCode", dto.getCafeCode());

          saveMarketRegistration(savedProduct, MarketType.CAFE24, ids);
        }

        successCount++;

      } catch (Exception e) {
        log.error("상품 저장 실패 (sbCode: {}): {}", dto.getSbCode(), e.getMessage());
        // 하나가 실패해도 멈추지 않고 다음 상품으로 진행 (필요 시 throw로 변경 가능)
      }
    }
    return successCount;
  }

  // 마켓 등록 정보 저장 헬퍼 메서드
  private void saveMarketRegistration(Product product, MarketType marketType, Map<String, Object> ids) {
    MarketRegistration registration = MarketRegistration.builder()
        .product(product) // ★ 저장된 부모 상품 객체 주입
        .marketType(marketType)
        .marketIdentifiers(ids)
        // CSV에 현재 판매가나 상태 정보가 있다면 currentMarketData에 추가 가능
        .build();

    marketRegistrationRepository.save(registration);
  }

  // --- 유틸 메서드 (숫자 변환, 공백 체크 등) ---
  private BigDecimal parseDecimal(String val) {
    if (val == null || val.isBlank()) return BigDecimal.ZERO;
    try {
      // 쉼표(,) 제거 후 변환
      return new BigDecimal(val.replaceAll(",", "").trim());
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  private Integer parseInt(String val) {
    if (val == null || val.isBlank()) return 0;
    try {
      // 소수점이 있을 경우 정수로 변환 (예: 1.0 -> 1)
      double d = Double.parseDouble(val.replaceAll(",", "").trim());
      return (int) d;
    } catch (Exception e) {
      return 0;
    }
  }

  private boolean hasText(String val) {
    return val != null && !val.isBlank() && !val.equals("nan"); // 'nan' 문자열 체크 추가
  }
}