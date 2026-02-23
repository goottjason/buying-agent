package com.sbshop.agent.core.domain.user.repository;

import com.sbshop.agent.core.domain.user.model.UserPreference;
import java.util.Optional;

public interface UserPreferenceRepository {
  UserPreference save(UserPreference userPreference);
  Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId);
}
