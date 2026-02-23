package com.sbshop.agent.api.product.processor;

import com.sbshop.agent.core.domain.market.MarketRegistration;
import com.sbshop.agent.core.domain.market.enums.MarketType;
import com.sbshop.agent.core.domain.product.Product;
import com.sbshop.agent.core.domain.product.ProductFinder;
import com.sbshop.agent.core.domain.product.port.Cafe24ProductDto;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncProcessor {

  private final ProductFinder productFinder;
  private final MarketProductPort marketProductPort; // 어댑터(Cafe24ProductAdapter)가 주입됩니다.

  // (MarketRegistrationRepository는 도메인 영역에 만들어두셨다고 가정합니다)
  // private final MarketRegistrationRepository registrationRepository;

  @Transactional
  public void syncWithCafe24(String sku, String cafe24ProductNo) {
    log.info("상품({})과 카페24({}) 동기화 시작", sku, cafe24ProductNo);

    // 1. 우리 DB에서 상품 찾기
    Product product = productFinder.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다."));

    // 2. 외부 마켓(카페24)에서 정확한 정보 가져오기
    Cafe24ProductDto cafe24Data = marketProductPort.getProductDetails(cafe24ProductNo);

    // 3. 우리 DB 상품 정보 업데이트 (HTML 등 덮어쓰기)
    // (NOTE: Product 엔티티에 updateDetailHtml() 같은 비즈니스 메서드를 만들어두시면 좋습니다)
    // product.updateDetailHtml(cafe24Data.detailHtml());

    // 4. 카페24에 "우리 시스템에서 동기화함" 도장 찍기
    String syncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String syncMessage = "[Managed by SB-Agent] Last Sync: " + syncTime;
    marketProductPort.updateSyncMemo(cafe24ProductNo, syncMessage);

    // 5. MarketRegistration (연동 정보) 남기기 혹은 갱신하기
    MarketRegistration registration = MarketRegistration.builder()
        .product(product)
        .marketType(MarketType.CAFE24)
        .marketProductName(product.getName())
        // 카페24 상품번호를 식별자로 저장합니다.
        .marketIdentifiers(Map.of("product_no", cafe24ProductNo))
        .build();

    registration.markAsSynced(); // isSynced = true, lastSyncedAt = now() 처리

    // registrationRepository.save(registration);

    log.info("동기화 완벽하게 종료되었습니다!");
  }
}