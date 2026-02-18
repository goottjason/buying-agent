package com.sbshop.agent.core.domain.product;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "goottjason", name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_id")
  private Long id;

  // --- 기본 정보 ---
  @Column(name = "sb_code", unique = true, nullable = false)
  private String sbCode; // 상품관리코드

  @Column(name = "kor_name", nullable = false)
  private String korName;

  @Column(name = "eng_name")
  private String engName;

  @Column(name = "category_name")
  private String categoryName; // (CSV: ctgy)

  // --- 소싱 정보 ---
  @Column(name = "sourcing_mkt", length = 20)
  private String sourcingMkt; // (CSV: mkt - 예: COK)

  @Column(name = "source_url", length = 1000)
  private String sourceUrl;

  @Column(name = "cost_price", precision = 15, scale = 2)
  private BigDecimal costPrice;

  @Column(name = "exchange_rate", precision = 10, scale = 2)
  private BigDecimal exchangeRate;

  @Column(name = "shipping_price", precision = 15, scale = 2)
  private BigDecimal shippingPrice;

  @Column(name = "margin_rate", precision = 5, scale = 2)
  private BigDecimal marginRate;

  @Column(name = "final_sale_price", precision = 15, scale = 0)
  private BigDecimal finalSalePrice;

  // --- 재고 및 상세 ---
  @Column(name = "stock_quantity")
  private Integer stockQuantity;

  @Column(name = "weight", precision = 10, scale = 2)
  private BigDecimal weight;

  @Column(name = "package_info")
  private String packageInfo;

  @Column(name = "package_quantity")
  private Integer packageQuantity;

  @Lob
  @Column(name = "html_content", columnDefinition = "LONGTEXT")
  private String htmlContent;

  @Column(name = "memo", columnDefinition = "TEXT")
  private String memo;

  // --- 5. Audit Log ---
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder
  public Product(String sbCode, String korName, String engName, String sourceUrl,
      BigDecimal costPrice, BigDecimal exchangeRate, BigDecimal shippingPrice,
      BigDecimal marginRate, BigDecimal finalSalePrice, Integer stockQuantity,
      BigDecimal weight, String packageInfo, String htmlContent, String memo) {
    this.sbCode = sbCode;
    this.korName = korName;
    this.engName = engName;
    this.sourceUrl = sourceUrl;
    this.costPrice = costPrice;
    this.exchangeRate = exchangeRate;
    this.shippingPrice = shippingPrice;
    this.marginRate = marginRate;
    this.finalSalePrice = finalSalePrice;
    this.stockQuantity = stockQuantity;
    this.weight = weight;
    this.packageInfo = packageInfo;
    this.htmlContent = htmlContent;
    this.memo = memo;
  }
}