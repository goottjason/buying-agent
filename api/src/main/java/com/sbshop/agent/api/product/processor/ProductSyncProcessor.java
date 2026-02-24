package com.sbshop.agent.api.product.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationRecorder;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.MarketPortFactory;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.model.vo.PriceInfo;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncProcessor {
  // 오케스트레이션(지휘)만 해야 할 ProductSyncProcessor
  // 클린 아키텍처에서 Processor(Application Service)는 "무엇(What)을 할지" 목차처럼 보여주기만 해야 합니다.

  private final ProductFinder productFinder;
  private final MarketPortFactory portFactory;
  private final ObjectMapper objectMapper;
  private final MarketRegistrationRecorder registrationRecorder;

  @Transactional
  public void syncMarketProduct(String sku, MarketType marketType) {
    log.info("단건 매칭 시작 - SKU: {}, Market: {}", sku, marketType);

    // 0. sku로 DB에서 상품 가져옴
    Product product = productFinder.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));

    // 1. 팩토리에서 해당 마켓에 맞는 통신 어댑터를 꺼내옴
    MarketProductPort port = portFactory.getPort(marketType);

    // 2. 어댑터에게 "이 SKU로 마켓 쪽 상품 번호 좀 찾아와!" 라고 시킵니다.
    MarketExtractedData marketData = port.getProductDetailsBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException(marketType + " 마켓에서 해당 SKU(" + sku + ")를 찾을 수 없습니다."));

    // 3. rawData를 보기좋게 출력
    try {
      String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(marketData.rawData());
      log.info("\n==================================================");
      log.info("📦 [{} 원본 데이터 확인 - SKU: {}]", marketType, sku);
      log.info("==================================================\n{}", prettyJson);
      log.info("==================================================");
    } catch (Exception e) {
      log.error("JSON 출력 실패", e);
    }

    // 4. 🚀 객체지향의 마법: 엔티티 스스로 업데이트하도록 위임 (한 줄로 끝!)
    product.syncFromMarket(marketData);

    // 5. 마켓 어드민 메모란에 매칭 마킹 남기기
    // 카페24의 경우 JSON 원본(rawData)에 'product_no'가 있으니 이를 꺼내 씁니다.
    String marketProductNo = String.valueOf(marketData.rawData().get("product_no"));
    port.updateSyncMemo(marketProductNo, "[SB-Agent] 시스템 매칭 완료");

    // 6. 연동 기록(MarketRegistration) 저장 및 JSON 바구니 통째로 붓기
    registrationRecorder.recordSyncSuccess(
        product,
        marketType,
        marketProductNo,
        marketData.rawData()
    );
  }


  // --------------------------------------------------------
  // [내부 헬퍼 메서드]
  // 시간 포맷팅 같은 기술적 디테일은 밑으로 빼서 핵심 흐름을 가리지 않게 합니다.
  // --------------------------------------------------------
  private String generateSyncMessage() {
    String syncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    return "[Managed by SB-Agent] Last Sync: " + syncTime;
  }
}