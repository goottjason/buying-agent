package com.sbshop.agent.core.domain.product;

import com.sbshop.agent.core.domain.product.MarketType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * 마켓 등록 정보 (1:N)
 * 하나의 상품이 여러 마켓에 등록될 때, 각 마켓별 ID(vendorItemId 등)와 상태를 관리함
 */
@Entity
@Table(schema = "goottjason", name = "market_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketRegistration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "registration_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false) // 필수값으로 지정
  private Product product;

  @Enumerated(EnumType.STRING)
  @Column(name = "market_type", nullable = false)
  private MarketType marketType;

  // 1. 식별자 정보 (변경되지 않는 ID값들: vendorItemId 등)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "market_identifiers", columnDefinition = "longtext")
  private Map<String, Object> marketIdentifiers = new HashMap<>();

  // 2. 현재 마켓 상태 스냅샷 (가격, 품절여부 등)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "current_market_data", columnDefinition = "longtext")
  private Map<String, Object> currentMarketData = new HashMap<>();

  @Column(name = "is_synced")
  private boolean isSynced;

  @Column(name = "last_synced_at")
  private LocalDateTime lastSyncedAt;

  // --- Audit Log ---
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt; // 연동 해제 시 기록

  @Builder
  public MarketRegistration(Product product, MarketType marketType, Map<String, Object> marketIdentifiers, Map<String, Object> currentMarketData) {
    this.product = product; // 생성 시점에 상품 주입 필수
    this.marketType = marketType;
    this.marketIdentifiers = marketIdentifiers != null ? marketIdentifiers : new HashMap<>();
    this.currentMarketData = currentMarketData != null ? currentMarketData : new HashMap<>();
    this.isSynced = true;
    this.lastSyncedAt = LocalDateTime.now();
  }

  // 연관관계 편의 메서드
  public void assignProduct(Product product) {
    this.product = product;
  }
}