package com.sbshop.agent.core.domain.product.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import com.sbshop.agent.core.domain.product.model.vo.ImageInfo;
import com.sbshop.agent.core.domain.product.model.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.model.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.model.vo.SourcingInfo;
import jakarta.persistence.*;
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

  @Builder
  public Product(String sku, String brand, String name, String originalName, CategoryType category,
      ProductSpec productSpec, SourcingInfo sourcingInfo, PriceInfo priceInfo,
      LogisticsInfo logisticsInfo, ImageInfo imageInfo, String searchKeywords, String detailHtml, String memo) {
    this.sku = sku;
    this.brand = brand;
    this.name = name;
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

  public void update(ProductUpdateCommand command) {

    // =====================================================================
    // 1. 기본 Flat 필드 업데이트 (null이 아닐 때만 덮어쓰기)
    // =====================================================================
    // if (command.sku() != null) this.sku = command.sku(); // 🔒 식별자는 보통 수정 불가
    if (command.brand() != null) this.brand = command.brand();
    if (command.name() != null) this.name = command.name();
    if (command.originalName() != null) this.originalName = command.originalName();
    if (command.category() != null) this.category = command.category();
    if (command.searchKeywords() != null) this.searchKeywords = command.searchKeywords();
    if (command.detailHtml() != null) this.detailHtml = command.detailHtml();
    if (command.memo() != null) this.memo = command.memo();

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
      if (command.costPrice() != null) priceBuilder.costPrice(command.costPrice());
      if (command.exchangeRate() != null) priceBuilder.exchangeRate(command.exchangeRate());
      if (command.deliveryFee() != null) priceBuilder.deliveryFee(command.deliveryFee());
      if (command.marginRate() != null) priceBuilder.marginRate(command.marginRate());
      if (command.salePrice() != null) priceBuilder.salePrice(command.salePrice());

      // 마지막에 딱 한 번만 build() 해서 통째로 갈아 끼웁니다!
      this.priceInfo = priceBuilder.build();
    }

    // =====================================================================
    // 3. LogisticsInfo (VO) 업데이트
    // =====================================================================
    boolean hasLogisticsUpdate = command.stock() != null || command.weight() != null || command.bundleQuantity() != null;
    if (hasLogisticsUpdate) {
      LogisticsInfo.LogisticsInfoBuilder logisticsBuilder = (this.logisticsInfo != null)
          ? this.logisticsInfo.toBuilder()
          : LogisticsInfo.builder();

      if (command.stock() != null) logisticsBuilder.stock(command.stock());
      if (command.weight() != null) logisticsBuilder.weight(command.weight());
      if (command.bundleQuantity() != null) logisticsBuilder.bundleQuantity(command.bundleQuantity());

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

      if (command.sourceImages() != null && !command.sourceImages().isEmpty()) {
        imageBuilder.sourceImages(command.sourceImages());
      }
      if (command.hostedImages() != null && !command.hostedImages().isEmpty()) {
        imageBuilder.hostedImages(command.hostedImages());
      }

      this.imageInfo = imageBuilder.build();
    }

    // =====================================================================
    // 5. ProductSpec (VO) 업데이트
    // =====================================================================
    boolean hasSpecUpdate = command.barcode() != null || command.capacity() != null || command.measureUnit() != null;
    if (hasSpecUpdate) {
      ProductSpec.ProductSpecBuilder specBuilder = (this.productSpec != null)
          ? this.productSpec.toBuilder()
          : ProductSpec.builder();

      if (command.barcode() != null) specBuilder.barcode(command.barcode());
      if (command.capacity() != null) specBuilder.capacity(command.capacity());
      if (command.measureUnit() != null) specBuilder.measureUnit(command.measureUnit());

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

      if (command.vendor() != null) sourcingBuilder.vendor(command.vendor());
      if (command.sourceUrl() != null) sourcingBuilder.sourceUrl(command.sourceUrl());
      if (command.manufacturer() != null) sourcingBuilder.manufacturer(command.manufacturer());
      if (command.origin() != null) sourcingBuilder.origin(command.origin());
      if (command.hsCode() != null) sourcingBuilder.hsCode(command.hsCode());

      this.sourcingInfo = sourcingBuilder.build();
    }
  }

  @Override
  public void delete() {
    super.delete();
  }
}