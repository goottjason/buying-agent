package com.sbshop.agent.core.domain.product.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.dto.ProductCreateCommand;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import com.sbshop.agent.core.domain.product.model.vo.ImageInfo;
import com.sbshop.agent.core.domain.product.model.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.model.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.model.vo.SourcingInfo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
@SQLDelete(sql = "UPDATE products SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Product extends BaseEntity {

  // --- 1. 기본 식별 정보 (Flat 필드) ---
  @Column(name = "sku", unique = true, nullable = false, length = 50)
  private String sku;

  @Column(name = "brand", length = 100)
  private String brand;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "base_name", nullable = true, length = 255)
  private String baseName;

  @Column(name = "original_name", length = 255)
  private String originalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", length = 50, columnDefinition = "varchar(50)")
  private CategoryType category;

  // --- 2. 묶음 정보 (Value Objects) ---
  @Embedded
  private ProductSpec productSpec;

  @Embedded
  private SourcingInfo sourcingInfo;

  @Embedded
  private PriceInfo priceInfo;

  @Embedded
  private LogisticsInfo logisticsInfo;

  @Embedded
  private ImageInfo imageInfo;

  // --- 3. 상세 설명 및 부가 정보 ---
  @Column(name = "search_keywords", length = 500)
  private String searchKeywords;

  @Lob
  @Column(name = "detail_html", columnDefinition = "LONGTEXT")
  private String detailHtml;

  @Column(name = "memo", length = 2000)
  private String memo;

  // 🚀 1. 빌더 어노테이션 제거!
  // 접근 제어자를 private으로 막아서 외부에서 new Product()를 못하게 원천 차단
  private Product(
      String sku, String brand, String name, String baseName, String originalName,
      CategoryType category, ProductSpec productSpec, SourcingInfo sourcingInfo,
      PriceInfo priceInfo, LogisticsInfo logisticsInfo, ImageInfo imageInfo,
      String searchKeywords, String detailHtml, String memo
  ) {
    this.sku = sku;
    this.brand = brand;
    this.name = name;
    this.baseName = baseName; // 💡 누락되었던 baseName 완벽 추가!
    this.originalName = originalName;
    this.category = category;
    this.productSpec = productSpec;
    this.sourcingInfo = sourcingInfo;
    this.priceInfo = priceInfo;
    this.logisticsInfo = logisticsInfo;
    this.imageInfo = imageInfo;
    this.searchKeywords = searchKeywords;
    this.detailHtml = detailHtml;
    this.memo = memo;
  }










  /**
   * 🚀 [도메인 로직] 평탄화된 Command를 받아 완벽한 마켓용 데이터로 스스로를 조립합니다.
   */
  public static Product create(String sku, ProductCreateCommand command) {

    // 1. 기본 텍스트 추출 (Null-Safe)
    String safeBrand = defaultString(command.brand());
    String safeBaseName = defaultString(command.baseName());
    String safeOriginalName = defaultString(command.originalName());

    // 🚀 2. 도메인 룰: 카테고리 매핑 및 HS Code 자동 할당
    CategoryType category = determineCategory(command.rawCategory());
    String hsCode = determineHsCode(category);

    // 3. 규격 및 단위 세팅
    int bundleQty = defaultIfNull(command.bundleQuantity(), 1);
    BigDecimal cap = defaultIfNull(command.capacity(), BigDecimal.ONE);
    MeasureUnit unit = defaultIfNull(command.measureUnit(), MeasureUnit.UNKNOWN);

    // 4. 마켓용 상품명 및 검색 키워드, HTML 생성
    String assembledName = assembleMarketName(safeBrand, safeBaseName, cap, unit, bundleQty);
    String searchKeywords = generateSearchKeywords(safeBrand, safeBaseName, safeOriginalName);
    String finalDetailHtml = buildDetailHtml(assembledName, safeOriginalName, bundleQty, cap, unit, command);

    // 🚀 5. 내장 객체(VO) 깔끔하게 분리 조립
    PriceInfo priceInfo = createPriceInfo(command);
    LogisticsInfo logisticsInfo = createLogisticsInfo(command, bundleQty);
    ProductSpec productSpec = createProductSpec(cap, unit);
    ImageInfo imageInfo = createImageInfo(command);
    SourcingInfo sourcingInfo = createSourcingInfo(command, safeBrand, hsCode);

    // 6. 최종 엔티티 반환
    return new Product(
        sku, safeBrand, assembledName, safeBaseName, safeOriginalName,
        category, productSpec, sourcingInfo, priceInfo, logisticsInfo,
        imageInfo, searchKeywords, finalDetailHtml, "{}"
    );
  }

  // =====================================================================
  // 🛠️ 도메인 내부 헬퍼 메서드 (가독성을 위한 분리)
  // =====================================================================

  private static CategoryType determineCategory(String rawCategory) {
    if (rawCategory == null) return CategoryType.UNKNOWN;
    if (rawCategory.contains("보충제") || rawCategory.contains("미네랄") || rawCategory.contains("비타민")) {
      return CategoryType.SUPPLEMENT; // 영양제 카테고리로 매핑
    }
    return CategoryType.UNKNOWN;
  }

  private static String determineHsCode(CategoryType category) {
    if (category == CategoryType.SUPPLEMENT) {
      return "2106.90.9099"; // 영양제 기본 세관 코드
    }
    return "";
  }

  private static PriceInfo createPriceInfo(ProductCreateCommand command) {
    BigDecimal cost = defaultIfNull(command.costPrice(), BigDecimal.ZERO);
    BigDecimal margin = defaultIfNull(command.marginRate(), BigDecimal.ZERO);
    BigDecimal sale = cost.multiply(BigDecimal.ONE.add(margin.divide(BigDecimal.valueOf(100))));

    return PriceInfo.builder()
        .costPrice(cost)
        .exchangeRate(BigDecimal.ONE)
        .deliveryFee(BigDecimal.ZERO)
        .marginRate(margin)
        .salePrice(sale)
        .build();
  }

  private static LogisticsInfo createLogisticsInfo(ProductCreateCommand command, int bundleQty) {
    return LogisticsInfo.builder()
        .stock(command.isAvailable() ? 999 : 0)
        .weight(defaultIfNull(command.weight(), BigDecimal.ZERO))
        .bundleQuantity(bundleQty)
        .build();
  }

  private static ProductSpec createProductSpec(BigDecimal cap, MeasureUnit unit) {
    return ProductSpec.builder()
        .barcode("")
        .capacity(cap)
        .measureUnit(unit)
        .build();
  }

  private static ImageInfo createImageInfo(ProductCreateCommand command) {
    return ImageInfo.builder()
        .sourceImages(command.sourceImages() != null ? command.sourceImages() : new ArrayList<>())
        .hostedImages(command.hostedImages() != null ? command.hostedImages() : new ArrayList<>())
        .build();
  }

  private static SourcingInfo createSourcingInfo(ProductCreateCommand command, String brand, String hsCode) {
    return SourcingInfo.builder()
        .vendor(command.vendor() != null ? command.vendor() : VendorType.IHB)
        .sourceUrl(defaultString(command.sourceUrl()))
        .manufacturer(brand)
        .origin(command.origin() != null ? command.origin() : "상세설명 참조")
        .hsCode(hsCode) // 💡 내부에서 결정된 hsCode 꽂아넣기!
        .build();
  }

  private static String generateSearchKeywords(String brand, String baseName, String originalName) {
    String keywords = String.format("%s,%s,%s", brand, baseName, originalName)
        .replaceAll(",,", ",").replaceAll("^,|,$", "");
    return keywords.length() > 500 ? keywords.substring(0, 499) : keywords;
  }

  private static String buildDetailHtml(String name, String originalName, int bundleCount, BigDecimal capacity, MeasureUnit unit, ProductCreateCommand command) {
    List<String> hosted = command.hostedImages() != null ? command.hostedImages() : new ArrayList<>();
    String mainImg = hosted.isEmpty() ? "" : hosted.get(0);
    List<String> addImgs = hosted.size() > 1 ? hosted.subList(1, hosted.size()) : new ArrayList<>();
    return generateTemplateHtml(name, originalName, bundleCount, capacity, unit, mainImg, addImgs, command.rawSourceHtml());
  }

  // (Null 방어 유틸리티)
  private static String defaultString(String str) { return str != null ? str : ""; }
  private static <T> T defaultIfNull(T value, T defaultValue) { return value != null ? value : defaultValue; }

    private static String assembleMarketName(String brand, String baseName, BigDecimal capacity, MeasureUnit unit, int bundleCount) {
    String unitDesc = unit != null && unit != MeasureUnit.UNKNOWN ? unit.getDescription() : "";
    int capInt = capacity.intValue(); // 깔끔한 출력을 위해 정수 변환
    return String.format("%s %s, %d%s, %d개", brand, baseName, capInt, unitDesc, bundleCount)
        .replaceAll(" ,", ",")
        .trim();
  }

  private static String generateTemplateHtml(
      String name, String originalName, int bundleCount, BigDecimal capacity,
      MeasureUnit measureUnit, String mainImageUrl, List<String> additionalImageUrls, String rawSourceHtml
  ) {
    StringBuilder sb = new StringBuilder();
    int capInt = capacity.intValue();

    sb.append(
        "<img src=\"http://ai.esmplus.com/shouldbe2480/notice/sb_top.png\" style=\"margin:0 auto; display:block; max-width:100%;\"><br/><br/>");
    sb.append("<div style=\"text-align: center; margin-bottom: 10px;\">")
        .append("<span style=\"font-size: 22px; color: #00B0A2; font-weight: bold;\">")
        .append(name)
        .append("</span><br/>")
        .append("<span style=\"font-size: 18px; color: #555;\">")
        .append(originalName != null ? originalName : "")
        .append("</span></div><br/><br/>");

    sb.append("<div style=\"text-align: center; margin-bottom: 30px;\">")
        .append("<span style=\"font-size: 20px; color: #EF007C; font-weight: bold;\">")
        .append("[구성품] 총 ")
        .append(bundleCount)
        .append(" 묶음상품 (1개 당 ")
        .append(capInt)
        .append(measureUnit.getDescription())
        .append(")</span></div><br/>");

    if (mainImageUrl != null && !mainImageUrl.isEmpty()) {
      sb.append("<img src=\"")
          .append(mainImageUrl)
          .append("\" style=\"margin:0 auto; display:block; max-width:800px;\"><br/><br/>");
    }
    for (String addImg : additionalImageUrls) {
      sb.append("<img src=\"")
          .append(addImg)
          .append("\" style=\"margin:0 auto; display:block; max-width:800px;\"><br/><br/>");
    }

    sb.append(
            "<div style=\"text-align: left; color: #636363; font-size: 16px; line-height: 1.6; max-width: 800px; margin: 0 auto;\">")
        .append(rawSourceHtml != null ? rawSourceHtml : "")
        .append("</div><br/><br/>");
    sb.append(
        "<img src=\"http://ai.esmplus.com/shouldbe2480/notice/sb_bottom.png\" style=\"margin:0 auto; display:block; max-width:100%;\">");

    return sb.toString();
  }















  public void update(ProductUpdateCommand command) {

    // =====================================================================
    // 1. 기본 Flat 필드 업데이트 (null이 아닐 때만 덮어쓰기)
    // =====================================================================
    // if (command.sku() != null) this.sku = command.sku(); // 🔒 식별자는 보통 수정 불가
    if (command.brand() != null) {
      this.brand = command.brand();
    }
    if (command.name() != null) {
      this.name = command.name();
    }
    if (command.baseName() != null) {
      this.baseName = command.baseName();
    }
    if (command.originalName() != null) {
      this.originalName = command.originalName();
    }
    if (command.category() != null) {
      this.category = command.category();
    }
    if (command.searchKeywords() != null) {
      this.searchKeywords = command.searchKeywords();
    }
    if (command.detailHtml() != null) {
      this.detailHtml = command.detailHtml();
    }
    if (command.memo() != null) {
      this.memo = command.memo();
    }

    // =====================================================================
    // 2. PriceInfo (VO) 업데이트
    // =====================================================================
    boolean hasPriceUpdate = command.costPrice() != null || command.exchangeRate() != null ||
        command.deliveryFee() != null || command.marginRate() != null ||
        command.salePrice() != null;
    if (hasPriceUpdate) {
      // 🚀 핵심 기술: 완성본(build)을 바로 만들지 않고, 조립 중인 '빌더 객체'만 먼저 꺼냅니다.
      PriceInfo.PriceInfoBuilder priceBuilder = (this.priceInfo != null)
          ? this.priceInfo.toBuilder()
          : PriceInfo.builder();

      // 입력 들어온 값들만 빌더에 쏙쏙 끼워 넣습니다.
      if (command.costPrice() != null) {
        priceBuilder.costPrice(command.costPrice());
      }
      if (command.exchangeRate() != null) {
        priceBuilder.exchangeRate(command.exchangeRate());
      }
      if (command.deliveryFee() != null) {
        priceBuilder.deliveryFee(command.deliveryFee());
      }
      if (command.marginRate() != null) {
        priceBuilder.marginRate(command.marginRate());
      }
      if (command.salePrice() != null) {
        priceBuilder.salePrice(command.salePrice());
      }

      // 마지막에 딱 한 번만 build() 해서 통째로 갈아 끼웁니다!
      this.priceInfo = priceBuilder.build();
    }

    // =====================================================================
    // 3. LogisticsInfo (VO) 업데이트
    // =====================================================================
    boolean hasLogisticsUpdate =
        command.stock() != null || command.weight() != null || command.bundleQuantity() != null;
    if (hasLogisticsUpdate) {
      LogisticsInfo.LogisticsInfoBuilder logisticsBuilder = (this.logisticsInfo != null)
          ? this.logisticsInfo.toBuilder()
          : LogisticsInfo.builder();

      if (command.stock() != null) {
        logisticsBuilder.stock(command.stock());
      }
      if (command.weight() != null) {
        logisticsBuilder.weight(command.weight());
      }
      if (command.bundleQuantity() != null) {
        logisticsBuilder.bundleQuantity(command.bundleQuantity());
      }

      this.logisticsInfo = logisticsBuilder.build();
    }

    // =====================================================================
    // 4. ImageInfo (VO) 업데이트
    // =====================================================================
    boolean hasImageUpdate = command.sourceImages() != null || command.hostedImages() != null;
    if (hasImageUpdate) {
      ImageInfo.ImageInfoBuilder imageBuilder = (this.imageInfo != null)
          ? this.imageInfo.toBuilder()
          : ImageInfo.builder();

      if (command.sourceImages() != null && !command.sourceImages()
          .isEmpty()) {
        imageBuilder.sourceImages(command.sourceImages());
      }
      if (command.hostedImages() != null && !command.hostedImages()
          .isEmpty()) {
        imageBuilder.hostedImages(command.hostedImages());
      }

      this.imageInfo = imageBuilder.build();
    }

    // =====================================================================
    // 5. ProductSpec (VO) 업데이트
    // =====================================================================
    boolean hasSpecUpdate =
        command.barcode() != null || command.capacity() != null || command.measureUnit() != null;
    if (hasSpecUpdate) {
      ProductSpec.ProductSpecBuilder specBuilder = (this.productSpec != null)
          ? this.productSpec.toBuilder()
          : ProductSpec.builder();

      if (command.barcode() != null) {
        specBuilder.barcode(command.barcode());
      }
      if (command.capacity() != null) {
        specBuilder.capacity(command.capacity());
      }
      if (command.measureUnit() != null) {
        specBuilder.measureUnit(command.measureUnit());
      }

      this.productSpec = specBuilder.build();
    }

    // =====================================================================
    // 6. SourcingInfo (VO) 업데이트
    // =====================================================================
    boolean hasSourcingUpdate = command.vendor() != null || command.sourceUrl() != null ||
        command.manufacturer() != null || command.origin() != null || command.hsCode() != null;
    if (hasSourcingUpdate) {
      SourcingInfo.SourcingInfoBuilder sourcingBuilder = (this.sourcingInfo != null)
          ? this.sourcingInfo.toBuilder()
          : SourcingInfo.builder();

      if (command.vendor() != null) {
        sourcingBuilder.vendor(command.vendor());
      }
      if (command.sourceUrl() != null) {
        sourcingBuilder.sourceUrl(command.sourceUrl());
      }
      if (command.manufacturer() != null) {
        sourcingBuilder.manufacturer(command.manufacturer());
      }
      if (command.origin() != null) {
        sourcingBuilder.origin(command.origin());
      }
      if (command.hsCode() != null) {
        sourcingBuilder.hsCode(command.hsCode());
      }

      this.sourcingInfo = sourcingBuilder.build();
    }
  }

  @Override
  public void delete() {
    super.delete();
  }

  /**
   * 전체 호스팅 이미지 목록 반환 (Null Safe)
   */
  public List<String> getHostedImages() {
    if (this.imageInfo == null || this.imageInfo.getHostedImages() == null) {
      return new ArrayList<>();
    }
    return this.imageInfo.getHostedImages();
  }

  /**
   * 0번 인덱스를 대표 이미지로 간주하여 반환 (Null Safe)
   */
  public String getRepImageUrl() {
    List<String> images = getHostedImages();
    if (images.isEmpty()) {
      return ""; // 또는 기본 이미지 URL
    }
    return images.get(0); // 0번째 인덱스가 대표 이미지!
  }


  /*
  // 🚀 [도메인 로직] 미등록 마켓 꼬리표 떼기 (동기화 성공 시)
  public void removeUnregisteredMark(MarketType marketType) {
    if (this.memo == null || this.memo.isBlank()) return;

    Map<String, Object> memoMap = parseMemoSafe();
    List<String> unregisteredMarkets = (List<String>) memoMap.get("미등록");

    if (unregisteredMarkets != null && unregisteredMarkets.contains(marketType.name())) {
      unregisteredMarkets.remove(marketType.name());
      if (unregisteredMarkets.isEmpty()) {
        memoMap.remove("미등록");
      }
      this.memo = JsonUtils.toJson(memoMap);
    }
  }

  // 🚀 [도메인 로직] 미등록 마켓 꼬리표 붙이기 (마켓에 없을 시)
  public void markAsUnregistered(MarketType marketType) {
    Map<String, Object> memoMap = parseMemoSafe();
    List<String> unregisteredMarkets = (List<String>) memoMap.computeIfAbsent("미등록", k -> new ArrayList<>());

    if (!unregisteredMarkets.contains(marketType.name())) {
      unregisteredMarkets.add(marketType.name());
      this.memo = JsonUtils.toJson(memoMap);
    }
  }

  // [내부 헬퍼] 평문 메모 하위호환성을 고려한 안전한 파싱
  private Map<String, Object> parseMemoSafe() {
    if (this.memo == null || this.memo.isBlank()) return new HashMap<>();
    try {
      return JsonUtils.toMap(this.memo);
    } catch (Exception e) {
      Map<String, Object> legacyMap = new HashMap<>();
      legacyMap.put("userMemo", this.memo);
      return legacyMap;
    }
  }*/

}