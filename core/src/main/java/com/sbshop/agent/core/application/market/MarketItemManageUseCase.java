package com.sbshop.agent.core.application.market;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationReader;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.MarketClientRouter;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketItemManageUseCase {

  private final MarketRegistrationReader registrationReader;
  private final MarketClientRouter clientRouter;

  /**
   * [API 1번용] DB에 저장된 마켓 상세 데이터(JSON)를 0.01초 만에 꺼내옵니다.
   */
  @Transactional(readOnly = true)
  public MarketItemInfo getLocalMarketRegistration(Long productId, MarketType marketType) {
    // 1. DB에서 엔티티를 꺼냄
    MarketRegistration registration = registrationReader.readByProductIdAndMarketType(productId, marketType);

    // 2. 해당 마켓의 번역기(Client)를 찾음
    MarketClient client = clientRouter.getClient(marketType);

    // 3. 엔티티 안에 들어있는 날것의 Map을 던져서 예쁜 공통 규격으로 번역받음!
    return client.parseLocalData(registration.getMarketDetailedInfo());
  }

  /**
   * [API 2번용] 사용자가 '최신화' 버튼을 눌렀을 때 외부 API를 찔러 데이터를 갱신합니다.
   */
  @Transactional
  public MarketItemInfo syncLiveMarketData(Long productId, MarketType marketType) {

    // 1. 우리 DB에서 대상 연동 객체를 찾습니다.
    MarketRegistration registration = registrationReader.readByProductIdAndMarketType(productId, marketType);

    // 2. 라우터를 통해 통신 클라이언트(ex: 쿠팡 클라이언트)를 가져옵니다.
    MarketClient client = clientRouter.getClient(marketType);

    // 3. 외부 마켓 API를 찔러서 최신 데이터를 추출합니다!
    MarketItemInfo extractedData = client.extractMarketItem(registration.getMarketItemId());

    // 4. 더티 체킹을 통해 로컬 DB 업데이트!
    registration.update(extractedData.toRegistrationUpdateCommand());

    // 5. 방금 최신화된 순수 도메인 엔티티를 반환!
    return extractedData;
  }
}