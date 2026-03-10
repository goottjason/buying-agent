package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.api.product.dto.ImageUpdateRequest;
import com.sbshop.agent.api.product.dto.PriceStockUpdateRequest;
import com.sbshop.agent.api.product.dto.ProductDetailResponse;
import com.sbshop.agent.api.product.dto.ProductGridResponse;
import com.sbshop.agent.core.application.product.ProductManageUseCase;
import com.sbshop.agent.core.application.product.ProductSearchUseCase;
import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductSearchUseCase productSearchUseCase;
  private final ProductManageUseCase productManageUseCase;

  /**
   * 상품 목록 조회 (그리드용): 상품정보와 마켓아이템의 아이디 목록을 가져와 그리드를 채움
   * GET /api/products?page=0&size=50
   */
  @GetMapping
  public CommonResponse<Page<ProductGridResponse>> searchProducts(Pageable pageable) {

    log.info("상품 그리드 목록 조회 요청 - 페이징: {}", pageable);

    // 1. UseCase에서 도메인 엔티티 묶음을 받아옴
    Page<ProductMarketAggregate> aggregates =
        productSearchUseCase.getProductsWithMarketItemIds(pageable);

    // 2. 화면 규격(GridResponse)으로 변환
    Page<ProductGridResponse> responsePage = aggregates
        .map(ProductGridResponse::from);

    return CommonResponse.ok(responsePage);
  }

  // 상품 ID로 상품정보를 가져와 모달을 채움
  @GetMapping("/{id}")
  public CommonResponse<ProductDetailResponse> getProductDetail(@PathVariable("id") Long id) {
    log.info("상품 상세 조회 요청 - ID: {}", id);

    ProductMarketAggregate aggregate = productSearchUseCase.getProductWithMarketItemIds(id);
    ProductDetailResponse response = ProductDetailResponse.from(aggregate);

    return CommonResponse.ok(response);
  }

  /**
   * 상품 가격/재고 단건 수정 및 마켓 동기화
   * PUT /api/products/100/price-stock
   */
  @PutMapping("/{id}/price-stock")
  public CommonResponse<Void> updatePriceAndStock(
      @PathVariable("id") Long id,
      @RequestBody PriceStockUpdateRequest request
  ) {
    log.info("상품 가격/재고 수정 및 동기화 요청 - ID: {}, 요청값: {}", id, request);

    productManageUseCase.updateAndBroadcastPriceStock(id, request.price(), request.stock());

    return CommonResponse.ok(null);
  }

  /**
   * 상품 이미지 및 상세 HTML 수정 + 마켓 브로드캐스트
   * PUT /api/products/100/images
   */
  @PutMapping("/{id}/images")
  public CommonResponse<Void> updateImagesAndHtml(
      @PathVariable("id") Long id,
      @RequestBody ImageUpdateRequest request
  ) {
    log.info("상품 이미지/HTML 수정 및 동기화 요청 - ID: {}, 이미지 수: {}", id, request.sourceImages().size());

    // UseCase 호출 (클라우드 업로드 -> detailHtml 및 hostedImages 치환 -> DB 업데이트 -> 마켓 반영)
    productManageUseCase.updateAndBroadcastImagesAndHtml(id, request.sourceImages());

    return CommonResponse.ok(null);
  }
}