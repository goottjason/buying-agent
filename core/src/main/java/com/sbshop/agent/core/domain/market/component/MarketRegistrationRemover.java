package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarketRegistrationRemover {

  private final MarketRegistrationRepository registrationRepository; // JPA 리포지토리

  /**
   * 마켓 상품 ID를 기반으로 우리 DB의 매핑 정보를 삭제합니다.
   */
  @Transactional
  public void deleteByProductId(Long productId) {
    // JPA Repository에 위임
    registrationRepository.deleteByProductId(productId);
  }
}