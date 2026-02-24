package com.sbshop.agent.core.domain.market.component;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
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
  public void recordSyncSuccess(Product product, MarketType marketType, String marketProductNo, Map<String, Object> rawData) {
    MarketRegistration registration = finder.findByProductIdAndMarketType(product.getId(), marketType)
        .orElseGet(() -> appender.append(
            MarketRegistration.builder()
                .product(product)
                .marketType(marketType)
                .marketProductName(product.getName())
                // 불변 Map(Map.of) 대신 가변 HashMap으로 감싸서 나중에 put()이 가능하도록 안전하게 처리합니다.
                .marketIdentifiers(new HashMap<>(Map.of("product_no", marketProductNo)))
                .build()
        ));
    // 🚀 새로 만들었든(appender), 기존에 있던 걸 찾았든(finder)
    // 항상 최신 식별자와 마켓 상세 JSON 데이터를 덮어씌워 줍니다. (JPA 더티 체킹 활용)
    registration.getMarketIdentifiers().put("product_no", marketProductNo);

    // (MarketRegistration 엔티티에 아래처럼 맵을 업데이트하는 비즈니스 메서드나 Setter를 만들어주세요)
    registration.updateDetailedInfo(rawData);

    registration.markAsSynced();
  }
}