package com.sbshop.agent.core.domain.user;

import com.sbshop.agent.core.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_preferences",
    // ADMIN 유저가 PRODUCT_GRID 메뉴에 대해 딱 1개의 세팅만 가지도록 고유 제약조건을 겁니다.
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "menu_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference extends BaseEntity {

  @Column(name = "user_id", nullable = false, length = 50)
  private String userId;

  @Column(name = "menu_id", nullable = false, length = 50)
  private String menuId;

  // AG Grid의 설정값은 길이가 매우 길어질 수 있는 JSON 문자열이므로 LONGTEXT로 잡습니다.
  @Column(name = "preference_data", columnDefinition = "LONGTEXT")
  private String preferenceData;

  @Builder
  public UserPreference(String userId, String menuId, String preferenceData) {
    this.userId = userId;
    this.menuId = menuId;
    this.preferenceData = preferenceData;
  }

  // 기존 설정값이 있을 때 덮어쓰기 위한 메서드
  public void updateData(String newPreferenceData) {
    this.preferenceData = newPreferenceData;
  }
}