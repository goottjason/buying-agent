package com.sbshop.agent.core.domain.product;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.product.dto.ProductSaveCommand;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.enums.VendorType;
import com.sbshop.agent.core.domain.product.vo.ImageInfo;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.vo.ProductSpec;
import com.sbshop.agent.core.domain.product.vo.SourcingInfo;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

  // ★ 1. 소프트 삭제 (Soft Delete) 메서드
  public void delete() {
    // BaseEntity에 status(ACTIVE, DELETED) 같은 필드가 있다고 가정합니다.
    // 만약 없다면, isDeleted 같은 boolean 필드를 쓰셔도 됩니다.
    // 여기서는 예시로 status 필드를 변경합니다.
    // this.status = Status.DELETED;
    super.markAsDeleted();
  }

  // ★ 2. 일괄 수정용 비즈니스 메서드
  // null이 들어온 값은 무시하고, 값이 있는 것만 업데이트합니다.
  public void updateBulkInfo(CategoryType category, String searchKeywords, String memo) {
    if (category != null) {
      this.category = category;
    }
    if (searchKeywords != null) {
      this.searchKeywords = searchKeywords;
    }
    if (memo != null) {
      this.memo = memo;
    }
  }

  // (참고) VO인 ProductSpec 안의 값을 바꾸려면 VO 자체를 새로 껴넣어야 합니다.
  public void updateSpec(ProductSpec newSpec) {
    if (newSpec != null) {
      this.productSpec = newSpec;
    }
  }

  // ★ 단건 상세 수정용 비즈니스 메서드
  public void updateDetail(String name, CategoryType category, BigDecimal salePrice, String memo, String detailHtml) {
    this.name = name;
    this.category = category;
    this.memo = memo;
    this.detailHtml = detailHtml;

    // PriceInfo는 VO(값 객체)이므로 부분 수정이 불가능합니다.
    // 기존의 원가, 배송비 등은 그대로 유지하고 판매가(salePrice)만 갈아끼운 새로운 객체를 통째로 덮어씌웁니다.
    if (this.priceInfo != null) {
      this.priceInfo = PriceInfo.builder()
          .costPrice(this.priceInfo.getCostPrice())
          .exchangeRate(this.priceInfo.getExchangeRate())
          .deliveryFee(this.priceInfo.getDeliveryFee())
          .marginRate(this.priceInfo.getMarginRate())
          .salePrice(salePrice != null ? salePrice : this.priceInfo.getSalePrice())
          .build();
    } else {
      // 기존 가격 정보가 아예 없었다면 새로 생성
      this.priceInfo = PriceInfo.builder().salePrice(salePrice).build();
    }
  }

  // ★ 엑셀 인라인 에디팅을 위한 '전체 필드' 덮어쓰기 메서드
  public void updateAllFields(ProductSaveCommand command) {
    this.name = command.getName();
    this.originalName = command.getOriginalName();
    this.brand = command.getBrand();
    this.category = command.getCategory();

    // VO(값 객체)들은 객체 자체를 통째로 갈아끼워줍니다.
    this.sourcingInfo = command.getSourcingInfo();
    this.productSpec = command.getProductSpec();
    this.priceInfo = command.getPriceInfo();
    this.logisticsInfo = command.getLogisticsInfo();

    this.searchKeywords = command.getSearchKeywords();
    this.memo = command.getMemo();
    // detailHtml은 엑셀 뷰에서 수정할 순 없지만 기존 값이 날아가지 않게 유지
    if (command.getDetailHtml() != null) {
      this.detailHtml = command.getDetailHtml();
    }
  }
}