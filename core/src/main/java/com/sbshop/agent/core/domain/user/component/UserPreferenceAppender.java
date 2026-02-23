// UserPreferenceAppender.java
package com.sbshop.agent.core.domain.user.component;

import com.sbshop.agent.core.domain.user.repository.UserPreferenceRepository;
import com.sbshop.agent.core.domain.user.model.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPreferenceAppender {
  private final UserPreferenceRepository repository;

  public UserPreference append(UserPreference userPreference) {
    return repository.save(userPreference);
  }
}