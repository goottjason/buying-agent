package com.sbshop.agent.infrastructure.user.repository;

import com.sbshop.agent.core.domain.user.UserPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreference, Long> {
  Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId);
}
