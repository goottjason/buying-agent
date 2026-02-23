package com.sbshop.agent.infrastructure.user.repository;
import com.sbshop.agent.core.domain.user.model.UserPreference;
import com.sbshop.agent.core.domain.user.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {
  private final UserPreferenceJpaRepository jpaRepository;

  @Override
  public UserPreference save(UserPreference userPreference) {
    return jpaRepository.save(userPreference);
  }

  @Override
  public Optional<UserPreference> findByUserIdAndMenuId(String userId, String menuId) {
    return jpaRepository.findByUserIdAndMenuId(userId, menuId);
  }
}