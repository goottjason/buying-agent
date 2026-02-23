package com.sbshop.agent.core.domain.user;

import java.util.Optional;

public interface UserPreferenceRepository {
  UserPreference save(UserPreference userPreference);
  Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId);
}
