package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.MarketClientRouter;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationWriter;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.component.ProductSanitizer;
import com.sbshop.agent.core.domain.product.component.ProductValidator;
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
  private final MarketClientRouter marketClientRouter;
  private final MarketRegistrationWriter marketRegistrationWriter;
  private final ProductSanitizer productSanitizer;
  private final ProductValidator productValidator;

  @Transactional
  public void publishToMarket(Long productId, MarketType marketType) {

    // =========================================================
    // 1. 로컬 DB에서 상품 조회
    // =========================================================
    Product product = productReader.read(productId);

    // =========================================================
    // 2. 정제 및 점검
    // =========================================================
    log.info("🔍 [점검] 상품 데이터 정제 및 검증 시작 - SKU: {}", product.getSku());

    // Step A. 레거시 데이터 마이그레이션
    productSanitizer.sanitizeForPublish(product);

    // Step B. 필수 값 널 체크 및 논리적 오류 검증 (실패 시 400 에러 발생)
    productValidator.validateForPublish(product);

    // =========================================================
    // 3. 마켓 클라이언트 연결 및 데이터 전송 (외부 API 호출)
    // =========================================================
    log.info("🚀 [마켓 전송] {} 마켓으로 상품 등록 API 호출을 시작합니다.", marketType);
    MarketClient client = marketClientRouter.getClient(marketType);

    // 💡 가장 무거운 작업: 마켓 스펙에 맞춰 데이터를 조립하고 전송한 뒤 식별자를 받아옴
    Map<String, String> marketIdentifiers = client.publish(product);

    // =========================================================
    // 4. 연동 기록(Registration) 생성 및 DB 저장
    // =========================================================
    // 💡 rawData(marketDetailedInfo)는 당장 알 수 없으므로 빈 맵을 넘기고 추후 Sync로 채움
    MarketRegistration registration = MarketRegistration.create(
        product,
        marketType,
        marketIdentifiers,
        new HashMap<>()
    );

    marketRegistrationWriter.write(registration);

    log.info("✅ 상품 [{}] - {} 마켓 연동 기록 저장 완료", product.getSku(), marketType);
  }
}