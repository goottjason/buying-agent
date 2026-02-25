package com.sbshop.agent.core.domain.common;

import com.sbshop.agent.core.domain.common.enums.EntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 자식 엔티티에게 공통 필드만 물려주는 부모 클래스임을 명시 (JPA 표준 어노테이션)
@MappedSuperclass
// DB에 Insert 또는 Update 시, 스프링이 그 이벤트를 감지(Listen)하여 특정 로직(CreatedDate 등)을 자동으로 실행
@EntityListeners(AuditingEntityListener.class)
// 모든 필드에 대한 getXXX() 메서드를 자동으로 생성
@Getter
// 파라미터가 없는 기본 생성자(public BaseEntity() {})를 자동으로 만들어주되, 접근 제어자를 protected로 제한
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 데이터 변경 충돌 방지 (Optimistic Lock)
  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(50)")
  private EntityStatus status = EntityStatus.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at", nullable = true)
  private LocalDateTime deletedAt;

  // Soft Delete 처리를 위한 공통 메서드
  public void delete() {
    this.status = EntityStatus.DELETED;
    this.deletedAt = LocalDateTime.now();
  }
}

/*
  [[ BaseEntity에 @NoArgsConstructor(access = AccessLevel.PROTECTED) 쓰는 이유 ]]
  1. JPA의 규칙: JPA는 DB에서 데이터를 가져와서 자바 객체로 만들 때(Reflection 사용) 반드시 기본 생성자가 필요
  2. Protected인 이유: JPA는 접근할 수 있으면서, 개발자는 Builder나 의도된 생성자를 통해서만 객체를 만들도록 제어 가능
 */

