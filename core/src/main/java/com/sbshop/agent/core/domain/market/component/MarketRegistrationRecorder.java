package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import java.time.LocalDateTime;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketRegistrationRecorder {

  private final MarketRegistrationFinder finder;
  private final MarketRegistrationAppender appender;

  /**
   * 마켓 연동 기록이 있으면 가져오고, 없으면 새로 만들어서 동기화 완료(Synced) 처리합니다.
   */
  public void recordSyncSuccess(
      Product product, MarketType marketType,
      Map<String, String> marketIdentifiers, Map<String, Object> rawData
  ) {

    // 1. 찾거나, 없으면 빈 컬렉션으로 초기화하여 생성
    MarketRegistration registration = finder.findByProductIdAndMarketType(product.getId(), marketType)
        .orElseGet(() -> appender.append(
            MarketRegistration.builder()
                .product(product)
                .marketType(marketType)
                .marketProductName(product.getName())
                .marketIdentifiers(new HashMap<>()) // 불변 Map 방어
                .build()
        ));

    // 2. 🚀 개발자님이 원하시던 우아한 Command 조립!
    MarketRegistrationUpdateCommand command = MarketRegistrationUpdateCommand.builder()
        .marketIdentifiers(marketIdentifiers)
        .marketDetailedInfo(rawData)
        .isSynced(true)                         // 동기화 상태 ON
        .lastSyncedAt(LocalDateTime.now())      // 현재 시간 기록
        .build();

    // 3. 엔티티에게 "이 커맨드대로 업데이트해!" 라고 지시
    registration.update(command);
  }
}