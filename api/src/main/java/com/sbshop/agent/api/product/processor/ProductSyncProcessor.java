package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.component.MarketRegistrationRecorder;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.component.MarketPortFactory;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketProductDto;
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
  private final MarketPortFactory portFactory; // 어댑터 대신 팩토리를 주입받음!
  private final MarketRegistrationRecorder registrationRecorder; // 지저분한 로직을 대신할 전담 객체

  @Transactional
  // [수정] 메서드명 변경 및 MarketType 추가
  public void syncMarketProduct(String sku, String marketProductNo, MarketType marketType) {
    log.info("상품({})과 마켓({} - {}) 동기화 시작", sku, marketType, marketProductNo);

    Product product = productFinder.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));

    // 🚀 핵심: 팩토리에서 해당 마켓에 맞는 통신 어댑터를 꺼내옵니다!
    MarketProductPort port = portFactory.getPort(marketType);

    // 이후 로직은 완전히 동일합니다. (어떤 마켓이든 조회/업데이트/메모 남기는 포맷은 같으니까요!)
    MarketProductDto marketData = port.getProductDetails(marketProductNo);

    product.update(ProductUpdateCommand.builder().detailHtml(marketData.detailHtml()).build());
    port.updateSyncMemo(marketProductNo, generateSyncMessage());
    registrationRecorder.recordSyncSuccess(product, marketType, marketProductNo);
  }


  /*@Transactional
  public void syncWithCafe24(String sku, String cafe24ProductNo) {
    log.info("상품({})과 카페24({}) 동기화 시작", sku, cafe24ProductNo);

    // 1. 내부 상품 조회
    Product product = productFinder.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));

    // 2. 외부 마켓(카페24)에서 정확한 정보 가져오기
    Cafe24ProductDto cafe24Data = marketProductPort.getProductDetails(cafe24ProductNo);

    // 3. 우리 DB 상품 정보 유연하게 업데이트! (원하는 필드만 쏙쏙 골라서 세팅)
    ProductUpdateCommand updateCmd = ProductUpdateCommand.builder()
        .detailHtml(cafe24Data.detailHtml())
        .build();

    product.update(updateCmd); // JPA 더티 체킹으로 트랜잭션 종료 시 자동 UPDATE 쿼리 발생

    // 4. 카페24에 "우리 시스템에서 동기화함" 도장 찍기
    // 3. 외부 마켓(Cafe24)에 동기화 완료 메모 남기기
    marketProductPort.updateSyncMemo(cafe24ProductNo, generateSyncMessage());

    // 4. 마켓 연동 이력(Registration) 기록 (디테일은 Recorder에게 위임!)
    registrationRecorder.recordSyncSuccess(product, MarketType.CAFE24, cafe24ProductNo);

    log.info("상품 [{}] 동기화 완벽하게 종료되었습니다!", sku);
  }*/

  // --------------------------------------------------------
  // [내부 헬퍼 메서드]
  // 시간 포맷팅 같은 기술적 디테일은 밑으로 빼서 핵심 흐름을 가리지 않게 합니다.
  // --------------------------------------------------------
  private String generateSyncMessage() {
    String syncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    return "[Managed by SB-Agent] Last Sync: " + syncTime;
  }
}