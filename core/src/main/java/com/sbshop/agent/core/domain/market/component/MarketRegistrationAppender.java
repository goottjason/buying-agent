package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketRegistrationAppender {
  private final MarketRegistrationRepository repository;

  public MarketRegistration append(MarketRegistration registration) {
    return repository.save(registration);
  }
  /**
   * 조립이 완료된 마켓 등록 정보(MarketRegistration)를 DB에 신규 저장합니다.
   */
  @Transactional
  public MarketRegistration save(MarketRegistration marketRegistration) {
    MarketRegistration savedRegistration = repository.save(marketRegistration);
    log.debug("   💾 마켓 등록 정보 영구 저장 완료 (ID: {})", savedRegistration.getId());
    return savedRegistration;
  }

  // 🗑️ 기존에 있던 파라미터 4개짜리 recordSyncSuccess(...) 메서드는 흔적도 없이 삭제합니다!
}