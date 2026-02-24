package com.sbshop.agent.infrastructure.persistence.user.repository;

import com.sbshop.agent.core.domain.user.model.UserPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreference, Long> {
  Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId);
}
