/*
package com.sbshop.agent.core.domain.sourcing.model;

import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import java.math.BigDecimal;
import lombok.Builder;
import java.util.List;

@Builder
public record SourcedProduct(
    String brand,
    String name, // 마켓 전송용 완성형 상품명 (브랜드+상품명+용량+단위+수량)
    String baseName,
    String originalName,
    String rawCategory,

    BigDecimal costPrice,
    BigDecimal marginRate,
    BigDecimal salePrice,

    Integer stock,
    BigDecimal weight,
    Integer bundleQuantity,

    String barcode,
    Integer capacity,
    MeasureUnit measureUnit,

    VendorType vendor,
    String sourceUrl,
    String manufacturer,
    String origin,
    String hsCode,

    List<String> sourceImages,
    List<String> hostedImages,

    String searchKeywords,
    String rawSourceHtml,
    String detailHtml
) {

  // 도메인 팩토리 메서드
  public static SourcedProduct create(
      String url, int price, String nameKr, String nameEn, String brand,
      String origin, String weight, String expirationDate,
      int capacity, MeasureUnit measureUnit,
      String mainImg, List<String> addImgs, String rawSourceHtml, boolean isAvailable, String rawCategory,
      int defaultMarginRate, int targetPrice
  ) {

    // 1. 도메인 내부 로직: 최적 묶음수 계산
    int optimalBundleCount = calculateOptimalBundle(price, defaultMarginRate, targetPrice);

    // 🚀 2. 도메인 내부 로직: 우리 회사만의 고유한 상세 HTML 템플릿 굽기
    String finalDetailHtml = generateTemplateHtml(
        nameKr, nameEn, optimalBundleCount, capacity, measureUnit, mainImg, addImgs, rawSourceHtml
    );
    return SourcedProduct.builder()
        .sourceUrl(url)
        .costPrice(price)
        .nameKr(nameKr)
        .nameEn(nameEn)
        .brand(brand)
        .origin(origin != null ? origin : "상세설명 참조")
        .weight(weight)
        .expirationDate(expirationDate)
        .capacity(capacity)
        .measureUnit(measureUnit)
        .mainImageUrl(mainImg)
        .additionalImageUrls(addImgs)
        .rawSourceHtml(rawSourceHtml) // 💡 원본 보관
        .detailHtml(finalDetailHtml)
        .isAvailable(isAvailable)
        .bundleCount(optimalBundleCount) // 기본 1개
        .marginRate(defaultMarginRate) // 기본 마진 30%
        .rawCategory(rawCategory)
        .build();
  }

  // 🚀 [2] 재구성 (프론트엔드에서 수정한 확정값 그대로 복원, 계산 안 함)
  public static SourcedProduct reconstruct(
      String url, int price, String nameKr, String nameEn, String brand,
      String origin, String weight, String expirationDate,
      int capacity, MeasureUnit measureUnit,
      String mainImg, List<String> addImgs, String rawSourceHtml,
      boolean isAvailable, int bundleCount, int marginRate, String rawCategory // 💡 stock 복구
  ) {
    return SourcedProduct.builder()
        .sourceUrl(url)
        .costPrice(price)
        .nameKr(nameKr)
        .nameEn(nameEn)
        .brand(brand)
        .origin(origin != null ? origin : "상세설명 참조")
        .weight(weight)
        .expirationDate(expirationDate)
        .capacity(capacity)
        .measureUnit(measureUnit)
        .mainImageUrl(mainImg)
        .additionalImageUrls(addImgs)
        .rawSourceHtml(rawSourceHtml) // 💡 원본 보관
        .detailHtml("") // 💡 어차피 바로 다시 구울 거라 일단 비워둠
        .isAvailable(isAvailable) // 💡 누락 복구
        .bundleCount(bundleCount)
        .marginRate(marginRate)
        .rawCategory(rawCategory)
        .build();
  }

  // 🚀 [3] 상태 전이 (새로운 Hosted 이미지로 HTML 재생성 및 불변 복사본 반환)
  public SourcedProduct withHostedImages(String hostedMainUrl, List<String> hostedAddUrls) {

    String rebuiltHtml = generateTemplateHtml(
        this.baseName, this.originalName, this.bundleCount,
        this.capacity, this.measureUnit,
        hostedMainUrl, hostedAddUrls, this.rawSourceHtml // 💡 새 CF 주소 투입
    );

    return SourcedProduct.builder()
        .sourceUrl(this.sourceUrl)
        .costPrice(this.costPrice)
        .nameKr(this.baseName)
        .nameEn(this.originalName)
        .brand(this.brand)
        .origin(this.origin)
        .weight(this.weight)
        .expirationDate(this.expirationDate)
        .capacity(this.capacity)
        .measureUnit(this.measureUnit)
        .mainImageUrl(hostedMainUrl)             // 💡 CF 메인 이미지로 교체
        .additionalImageUrls(hostedAddUrls)      // 💡 CF 추가 이미지로 교체
        .rawSourceHtml(this.rawSourceHtml)
        .detailHtml(rebuiltHtml)                 // 💡 새 CF 이미지로 구워진 완벽한 HTML 세팅!
        .isAvailable(this.stock)
        .bundleCount(this.bundleCount)
        .marginRate(this.marginRate)
        .rawCategory(this.rawCategory)
        .build();
  }

  */
/**
   * [도메인 내부 로직] 최종 마켓 판매가가 targetPrice(예: 60,000원)에 가장 근접하도록 묶음수를 계산합니다.
   *//*

  private static int calculateOptimalBundle(
      int costPrice, int marginRate, int targetPrice
  ) {
    if (costPrice <= 0) {
      return 1; // 원가가 0원이면 에러 방지용으로 1개 반환
    }

    // 1개당 마진이 포함된 예상 판매가 계산 (예: 15,000 * 1.3 = 19,500원)
    double pricePerItemWithMargin = costPrice * (1 + (marginRate / 100.0));

    // 타겟 가격에 맞추기 위한 필요 수량 계산 (예: 60,000 / 19,500 = 3.07 -> 반올림하여 3개)
    int calculatedBundle = (int) Math.round(targetPrice / pricePerItemWithMargin);

    // 아무리 비싸도 최소 1개는 팔아야 하므로 Math.max 처리
    return Math.max(1, calculatedBundle);
  }

  // 🚀 파서에서 넘어온 도메인 로직: 불필요한 이미지 제거 & 깔끔한 렌더링
  private static String generateTemplateHtml(
      String nameKr, String nameEn, int bundleCount, int capacity,
      MeasureUnit measureUnit, String mainImageUrl, List<String> additionalImageUrls, String rawSourceHtml
  ) {

    StringBuilder sb = new StringBuilder();

    // 1. 상단 공지 이미지 (불필요한 01~03 제거, sb_top만 유지)
    sb.append(
        "<img src=\"http://ai.esmplus.com/shouldbe2480/notice/sb_top.png\" style=\"margin:0 auto; display:block; max-width:100%;\"><br/><br/>");

    // 2. 상품명 영역
    sb.append("<div style=\"text-align: center; margin-bottom: 10px;\">")
        .append("<span style=\"font-size: 22px; color: #00B0A2; font-weight: bold;\">")
        .append(nameKr)
        .append("</span><br/>")
        .append("<span style=\"font-size: 18px; color: #555;\">")
        .append(nameEn)
        .append("</span>")
        .append("</div><br/><br/>");

    // 3. 구성품 정보 (계산된 묶음수와 파싱된 용량/단위 자동 주입)
    sb.append("<div style=\"text-align: center; margin-bottom: 30px;\">")
        .append("<span style=\"font-size: 20px; color: #EF007C; font-weight: bold;\">")
        .append("[구성품] 총 ")
        .append(bundleCount)
        .append(" 묶음상품 (1개 당 ")
        .append(capacity)
        .append(measureUnit.getDescription())
        .append(")")
        .append("</span></div><br/>");

    // 4. 대표 이미지 & 부가 이미지
    if (mainImageUrl != null && !mainImageUrl.isEmpty()) {
      sb.append("<img src=\"")
          .append(mainImageUrl)
          .append("\" style=\"margin:0 auto; display:block; max-width:800px;\"><br/><br/>");
    }
    if (additionalImageUrls != null) {
      for (String addImg : additionalImageUrls) {
        sb.append("<img src=\"")
            .append(addImg)
            .append("\" style=\"margin:0 auto; display:block; max-width:800px;\"><br/><br/>");
      }
    }

    // 5. 소싱처 원본 상세설명 덩어리
    sb.append(
            "<div style=\"text-align: left; color: #636363; font-size: 16px; line-height: 1.6; max-width: 800px; margin: 0 auto;\">")
        .append(rawSourceHtml != null ? rawSourceHtml : "")
        .append("</div><br/><br/>");

    // 6. 하단 공지 이미지 (불필요한 04~06 제거, sb_bottom만 유지)
    sb.append(
        "<img src=\"http://ai.esmplus.com/shouldbe2480/notice/sb_bottom.png\" style=\"margin:0 auto; display:block; max-width:100%;\">");

    return sb.toString();
  }
}*/
