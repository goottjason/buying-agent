package com.sbshop.agent.api.user.processor;

import com.sbshop.agent.core.domain.user.model.UserPreference;
import com.sbshop.agent.core.domain.user.component.UserPreferenceAppender;
import com.sbshop.agent.core.domain.user.component.UserPreferenceFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserPreferenceProcessor {

  private final UserPreferenceFinder finder;
  private final UserPreferenceAppender appender;

  // 고정된 임시 관리자 ID
  private static final String DEFAULT_USER_ID = "ADMIN";

  @Transactional
  public void saveOrUpdatePreference(String menuId, String preferenceData) {
    Optional<UserPreference> existingPref = finder.findByUserIdAndMenuId(DEFAULT_USER_ID, menuId);

    if (existingPref.isPresent()) {
      // 이미 설정이 있으면 내용만 덮어씁니다. (JPA의 더티 체킹으로 자동 업데이트 됨)
      existingPref.get().updateData(preferenceData);
    } else {
      // 처음 설정하는 거라면 새로 만들어서 저장합니다.
      UserPreference newPref = UserPreference.builder()
          .userId(DEFAULT_USER_ID)
          .menuId(menuId)
          .preferenceData(preferenceData)
          .build();
      appender.append(newPref);
    }
  }

  @Transactional(readOnly = true)
  public Optional<String> getPreferenceData(String menuId) {
    // 해당 메뉴의 설정값이 있으면 JSON 문자열만 쏙 빼서 돌려줍니다.
    return finder.findByUserIdAndMenuId(DEFAULT_USER_ID, menuId)
        .map(UserPreference::getPreferenceData);
  }
}