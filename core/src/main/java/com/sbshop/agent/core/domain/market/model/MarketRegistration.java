package com.sbshop.agent.core.domain.market.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
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
  private Map<String, String> marketIdentifiers = new HashMap<>();

  // 현재 마켓에 세팅되어 있는 상태값, 가격 등 (JSON)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "market_detailed_info", nullable = true, columnDefinition = "longtext")
  private Map<String, Object> marketDetailedInfo = new HashMap<>();

  @Column(name = "is_synced", nullable = false)
  private boolean isSynced = false;

  @Column(name = "last_synced_at", nullable = true)
  private LocalDateTime lastSyncedAt;

  @Builder
  public MarketRegistration(Product product, MarketType marketType, String marketProductName,
      Map<String, String> marketIdentifiers, Map<String, Object> marketDetailedInfo,
      boolean isSynced, LocalDateTime lastSyncedAt) { // 🚀 이 두 녀석을 파라미터로 뚫어줍니다!
    this.product = product;
    this.marketType = marketType;
    this.marketProductName = marketProductName;
    this.marketIdentifiers = marketIdentifiers != null ? marketIdentifiers : new HashMap<>();
    this.marketDetailedInfo = marketDetailedInfo != null ? marketDetailedInfo : new HashMap<>();
    // 빌더나 외부에서 넘겨준 값을 그대로 세팅합니다.
    this.isSynced = isSynced;
    this.lastSyncedAt = lastSyncedAt;
  }

  // 🚀 [신규 추가] 엔티티 생성은 내가 직접 통제한다!
  public static MarketRegistration create(Product product,
      MarketType marketType,
      Map<String, String> identifiers,
      Map<String, Object> rawData) {
    return MarketRegistration.builder()
        .product(product)
        .marketType(marketType)
        .marketProductName(product.getName()) // 원본 상품의 진짜 SKU
        .marketIdentifiers(identifiers)
        .marketDetailedInfo(rawData)
        .isSynced(true)              // 생성되자마자 동기화 성공 상태!
        .lastSyncedAt(LocalDateTime.now())
        .build();
  }

  // =====================================================================
  // 🚀 완벽하게 캡슐화된 Command 패턴 업데이트 메서드
  // =====================================================================
  public void update(MarketRegistrationUpdateCommand command) {

    // =====================================================================
    // 마켓 실제 상품명 업데이트
    // =====================================================================
    if (command.marketProductName() != null && !command.marketProductName().isBlank()) {
      this.marketProductName = command.marketProductName();
    }

    // =====================================================================
    // 1. 식별자 맵(Map) 병합 업데이트
    // =====================================================================
    if (command.marketIdentifiers() != null && !command.marketIdentifiers().isEmpty()) {
      // JPA에서 컬렉션이 null일 경우를 대비한 안전한 초기화
      if (this.marketIdentifiers == null) {
        this.marketIdentifiers = new java.util.HashMap<>();
      }
      // 기존 식별자(예: 스마트스토어의 다른 코드)는 유지하면서 새로 들어온 식별자만 덮어씁니다.
      this.marketIdentifiers.putAll(command.marketIdentifiers());
    }

    // =====================================================================
    // 2. 마켓 원본 상세 데이터 덮어쓰기
    // =====================================================================
    if (command.marketDetailedInfo() != null) {
      this.marketDetailedInfo = command.marketDetailedInfo();
    }

    // =====================================================================
    // 3. 동기화 상태 및 시간 업데이트 (markAsSynced 병합)
    // =====================================================================
    if (command.isSynced() != null) {
      this.isSynced = command.isSynced();
    }
    if (command.lastSyncedAt() != null) {
      this.lastSyncedAt = command.lastSyncedAt();
    }
  }
}