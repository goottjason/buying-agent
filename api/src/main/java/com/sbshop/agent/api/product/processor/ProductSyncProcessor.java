package com.sbshop.agent.api.product.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationRecorder;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.MarketPortFactory;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
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

    // 0. sku를 통해 DB에서 상품 가져옴
    // Product product = productFinder.findBySku(sku)
    //     .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));

    // (팩토리에서 각 포트를 가져오는 로직 - 팩토리 구현체도 이에 맞게 수정 필요)
    MarketProductReaderPort readerPort = portFactory.getReaderPort(marketType);
    MarketDataExtractorPort extractorPort = portFactory.getExtractorPort(marketType);
    MarketCommandPort commandPort = portFactory.getCommandPort(marketType);

    // 1단계: SKU로 마켓의 고유 상품번호 찾기
    String marketProductNo = readerPort.findMarketProductNoBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException(marketType + " 마켓에서 해당 SKU(" + sku + ")를 찾을 수 없습니다."));

    // 2단계: 찾아낸 고유 번호로 상세 데이터(HTML, 이미지 등) 추출해오기
    MarketExtractedData marketData = extractorPort.extractInitialProductData(marketProductNo);

    // 3. rawData를 보기좋게 출력
    /*try {
      String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(marketData.rawData());
      log.info("📦 [{} 원본 데이터 확인 - SKU: {}] \n", marketType, sku);
      log.info("{}", prettyJson);
    } catch (Exception e) {
      log.error("JSON 출력 실패", e);
    }*/

    // =========================================================================
    // 🚨 [정찰 모드] 스마트스토어 탐색 중 DB가 오염되는 것을 막기 위해 아래는 잠시 주석 처리!
    // =========================================================================
    /*if (marketData.isMasterData()) {
      ProductUpdateCommand command = ProductUpdateCommand.builder()
          .name(marketData.name())
          .originalName(marketData.originalName())
          .salePrice(marketData.salePrice())
          .stock(marketData.stock())
          .detailHtml(marketData.detailHtml())
          .hostedImages(marketData.images())
          .build();
      product.update(command);
    } else {
      log.info("⏩ [{}] 마켓은 보조 데이터이므로 Product 정보 덮어쓰기를 생략합니다.", marketType);
    }*/

    // 5. 마켓 어드민 메모란에 매칭 마킹 남기기
    // commandPort.updateSyncMemo(marketProductNo, "[SB-Agent] 시스템 매칭 완료");

    // 6. 연동 기록(MarketRegistration) 저장 및 JSON 바구니 통째로 붓기
    /*registrationRecorder.recordSyncSuccess(
        product,
        marketType,
        marketData.marketIdentifiers(),
        marketData.rawData()
    );*/
  }
}