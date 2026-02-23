// UserPreferenceFinder.java
package com.sbshop.agent.core.domain.user.component;

import com.sbshop.agent.core.domain.user.repository.UserPreferenceRepository;
import com.sbshop.agent.core.domain.user.model.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPreferenceFinder {
  private final UserPreferenceRepository repository;

  public Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId) {
    return repository.findByUserIdAndMenuId(userId, menuId);
  }
}