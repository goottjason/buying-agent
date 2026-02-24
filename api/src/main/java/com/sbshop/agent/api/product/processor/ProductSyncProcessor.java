package com.sbshop.agent.api.product.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.MarketPortFactory;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
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
  // private final MarketRegistrationRecorder registrationRecorder; // 지저분한 로직을 대신할 전담 객체

  @Transactional
  public void syncMarketProduct(String sku, MarketType marketType) {
    log.info("단건 매칭 시작 - SKU: {}, Market: {}", sku, marketType);

    // sku로 DB에서 상품 가져옴
    // Product product = productFinder.findBySku(sku)
    //     .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));

    // 1. 팩토리에서 해당 마켓에 맞는 통신 어댑터를 꺼내옴
    MarketProductPort port = portFactory.getPort(marketType);

    // 2. 어댑터에게 "이 SKU로 마켓 쪽 상품 번호 좀 찾아와!" 라고 시킵니다.
    MarketExtractedData marketData = port.getProductDetailsBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException(marketType + " 마켓에서 해당 SKU(" + sku + ")를 찾을 수 없습니다."));

    // 4. rawData를 보기좋게 출력
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

    // product.update(ProductUpdateCommand.builder().detailHtml(marketData.detailHtml()).build());
    // port.updateSyncMemo(marketProductNo, generateSyncMessage());
    // registrationRecorder.recordSyncSuccess(product, marketType, marketProductNo);
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