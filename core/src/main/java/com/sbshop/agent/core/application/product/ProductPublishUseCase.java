package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.MarketClientRouter;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationWriter;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPublishUseCase {
  private final ProductReader productReader;
  private final MarketClientRouter marketClientRouter; // 🚀 기존 라우터 재사용!
  // (만약 연동 기록을 Market 도메인에서 관리한다면 MarketRegistrationWriter 등으로 변경)
  private final MarketRegistrationWriter marketRegistrationWriter;

  @Transactional
  public void publishToMarket(Long productId, MarketType marketType) {
    // 1. 로컬 DB에서 완벽하게 세팅된 상품 엔티티 조회
    Product product = productReader.read(productId);

    // 2. 팩토리를 통해 해당 마켓의 전용 클라이언트(리모컨)를 가져옴
    MarketClient client = marketClientRouter.getClient(marketType);

    // 3. 마켓으로 데이터 전송! (가장 무거운 작업)
    // 💡 반환값으로 마켓에서 발급해준 '상품 번호(Market Item ID)'를 받음
    Map<String, String> marketIdentifiers = client.publish(product);

    // 4. 성공 시 연동 기록(Registration) DB에 저장
    // 🚀 4. 종원 님의 도메인 팩토리 메서드(create) 활용!
    // 💡 rawData(marketDetailedInfo)는 당장 알 수 없으므로 빈 맵을 넘겨줍니다. (나중에 Sync 버튼으로 채움)
    MarketRegistration registration = MarketRegistration.create(
        product,
        marketType,
        marketIdentifiers,
        new HashMap<>() // 💡 추후 GET API로 채워질 영역
    );

    marketRegistrationWriter.write(registration);
    log.info("✅ 상품 [{}] - {} 마켓 연동 기록 저장 완료", product.getSku(), marketType);
  }
}
