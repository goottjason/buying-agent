package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.repository.MarketRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRegistrationAppender {
  private final MarketRegistrationRepository repository;

  public MarketRegistration append(MarketRegistration registration) {
    return repository.save(registration);
  }
}