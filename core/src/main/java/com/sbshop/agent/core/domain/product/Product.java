package com.sbshop.agent.core.domain.product;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.product.enums.CategoryType;
import com.sbshop.agent.core.domain.product.enums.VendorType;
import com.sbshop.agent.core.domain.product.vo.LogisticsInfo;
import com.sbshop.agent.core.domain.product.vo.PriceInfo;
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
@SQLDelete(sql = "UPDATE products SET status = 'DELETED' WHERE id = ?")
public class Product extends BaseEntity {

  // 식별 정보
  @Column(name = "sku", unique = true, nullable = false, length = 50)
  private String sku;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "original_name", nullable = true, length = 255)
  private String originalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = true, length = 50)
  private CategoryType category;

  // 소싱처 정보
  @Enumerated(EnumType.STRING)
  @Column(name = "vendor", nullable = false, length = 50)
  private VendorType vendor;

  @Column(name = "source_url", nullable = true, length = 1000)
  private String sourceUrl;

  // 묶인 VO들
  @Embedded
  private PriceInfo priceInfo;

  @Embedded
  private LogisticsInfo logisticsInfo;

  // 상세
  @Lob
  @Column(name = "detail_html", nullable = true, columnDefinition = "longtext")
  private String detailHtml;

  @Column(name = "memo", nullable = true, length = 2000)
  private String memo;

  @Builder
  public Product(String sku, String name, String originalName, CategoryType category, VendorType vendor,
      String sourceUrl, PriceInfo priceInfo, LogisticsInfo logisticsInfo, String detailHtml, String memo) {
    this.sku = sku;
    this.name = name;
    this.originalName = originalName;
    this.category = category;
    this.vendor = vendor;
    this.sourceUrl = sourceUrl;
    this.priceInfo = priceInfo;
    this.logisticsInfo = logisticsInfo;
    this.detailHtml = detailHtml;
    this.memo = memo;
  }
}