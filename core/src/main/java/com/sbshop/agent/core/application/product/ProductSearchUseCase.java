package com.sbshop.agent.core.application.product;

// import com.sbshop.agent.api.product.dto.ProductGridResponse;

import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationReader;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchUseCase {

  private final ProductReader productReader;
  private final MarketRegistrationReader registrationReader;

  @Transactional(readOnly = true)
  public Page<ProductMarketAggregate> getProductsWithMarketItemIds(Pageable pageable) {

    // 1. 상품 목록을 페이징하여 조회 (예: 500개)
    Page<Product> productPage = productReader.readProducts(pageable);

    // 2. 조회된 500개 상품 ID만 추출
    List<Long> productIds = productPage.getContent()
        .stream()
        .map(Product::getId)
        .toList();

    // 3. 방어 로직: 만약 해당 페이지에 상품이 없으면 굳이 마켓 정보를 조회할 필요 없음
    if (productIds.isEmpty()) {
      return Page.empty(pageable);
    }

    // 4. 상품 ID 리스트로 마켓등록 레코드 리스트 조회
    List<MarketRegistration> registrations = registrationReader.readAllByProductIds(productIds);

    // 5. 가져온 레코드를 상품 ID를 기준으로 묶음 (메모리상에서 조립)
    // 결과: { 1L: [쿠팡 아이템ID, 카페24 아이템ID], 2L: [스마트스토어 아이템ID], ... }
    Map<Long, List<MarketRegistration>> regMap = registrations.stream()
        .collect(Collectors
            .groupingBy(reg -> reg.getProduct().getId()));

    // 6. Product와 MarketRegistration을 담아 Aggregate를 반환
    return productPage.map(product ->
        ProductMarketAggregate.builder()
            .product(product)
            // regMap에서 현재 상품 ID를 Key로 조회
            // 만약 연동 기록이 없다면 null 방지를 위해 빈 리스트(Collections.emptyList()) 추가
            .registrations(regMap
                .getOrDefault(product.getId(), Collections.emptyList()))
            .build()
    );
  }

  /**
   * 상품 단건 상세 조회 (상품 + 연동 마켓 정보 묶음)
   */
  @Transactional(readOnly = true)
  public ProductMarketAggregate getProductWithMarketItemIds(Long productId) {
    // 1. 상품 단건 조회 (없으면 예외 발생)
    Product product = productReader.read(productId);

    // 2. 해당 상품의 마켓 연동 기록 조회
    List<MarketRegistration> registrations = registrationReader.readAllByProductId(productId);

    // 3. 상자에 담아서 그대로 리턴
    return ProductMarketAggregate.builder()
        .product(product)
        .registrations(registrations)
        .build();
  }
}