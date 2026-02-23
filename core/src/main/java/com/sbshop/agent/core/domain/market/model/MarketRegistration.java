package com.sbshop.agent.core.domain.market.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "market_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
@SQLDelete(sql = "UPDATE market_registrations SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class MarketRegistration extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Enumerated(EnumType.STRING)
  @Column(name = "market_type", nullable = false, length = 50, columnDefinition = "varchar(50)")
  private MarketType marketType;

  // [핵심] API로 찾아온 해당 마켓의 실제 상품명 (매칭 검증용)
  @Column(name = "market_product_name", nullable = true, length = 255)
  private String marketProductName;

  // API 연동으로 찾아낸 마켓별 고유 식별자 모음 (JSON)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "market_identifiers", nullable = true, columnDefinition = "longtext")
  private Map<String, Object> marketIdentifiers = new HashMap<>();

  // 현재 마켓에 세팅되어 있는 상태값, 가격 등 (JSON)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "current_market_data", nullable = true, columnDefinition = "longtext")
  private Map<String, Object> currentMarketData = new HashMap<>();

  @Column(name = "is_synced", nullable = false)
  private boolean isSynced = false;

  @Column(name = "last_synced_at", nullable = true)
  private LocalDateTime lastSyncedAt;

  @Builder
  public MarketRegistration(Product product, MarketType marketType, String marketProductName,
      Map<String, Object> marketIdentifiers, Map<String, Object> currentMarketData) {
    this.product = product;
    this.marketType = marketType;
    this.marketProductName = marketProductName;
    this.marketIdentifiers = marketIdentifiers != null ? marketIdentifiers : new HashMap<>();
    this.currentMarketData = currentMarketData != null ? currentMarketData : new HashMap<>();
    // 빌더로 최초 생성 시, 실제 API 성공 전까지는 false로 둡니다.
    this.isSynced = false;
  }

  // API 통신 성공 시 호출할 메서드
  public void markAsSynced() {
    this.isSynced = true;
    this.lastSyncedAt = LocalDateTime.now();
  }

  // 상태나 가격 변경으로 인해 재동기화가 필요해졌을 때 호출
  public void requireSync() {
    this.isSynced = false;
  }
}