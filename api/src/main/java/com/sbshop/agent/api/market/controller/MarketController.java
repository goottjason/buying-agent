package com.sbshop.agent.api.market.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.api.market.dto.MarketDetailResponse;
import com.sbshop.agent.core.application.market.MarketItemManageUseCase;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/products/{productId}/markets/{marketType}")
@RequiredArgsConstructor
public class MarketController {
  private final MarketItemManageUseCase marketItemManageUseCase;

  /**
   * 1. 그리드에서 마켓 코드 클릭 시 DB에서 가져와 모달을 채움
   * GET /api/products/100/markets/COUPANG/local
   */
  @GetMapping("/local")
  public CommonResponse<MarketDetailResponse> getLocalMarketData(
      @PathVariable("productId") Long productId,
      @PathVariable("marketType") MarketType marketType
  ) {
    log.info("마켓 데이터 로컬 조회 - 상품ID: {}, 마켓: {}", productId, marketType);

    // 1. UseCase에서 깔끔하게 번역된 Info 객체를 받아옴
    MarketItemInfo info = marketItemManageUseCase.getLocalMarketRegistration(productId, marketType);

    // 2. 화면용 Response 규격으로 변환합니다.
    MarketDetailResponse response = MarketDetailResponse.from(info);

    return CommonResponse.ok(response);
  }

  /**
   * 2. 외부 마켓 API 강제 동기화 (팝업에서 '최신 상태 불러오기 🔄' 클릭 시)
   * POST /api/products/100/markets/COUPANG/sync
   */
  @PostMapping("/sync")
  public CommonResponse<MarketDetailResponse> syncLiveMarketData(
      @PathVariable("productId") Long productId,
      @PathVariable("marketType") MarketType marketType
  ) {
    log.info("마켓 데이터 강제 동기화 요청 - 상품ID: {}, 마켓: {}", productId, marketType);

    // 1. UseCase에서 외부 통신 및 DB 업데이트를 마친 최신 엔티티를 받아옵니다.

    MarketItemInfo info = marketItemManageUseCase.syncLiveMarketData(
        productId, marketType);

    // 2. 화면용 Response 규격으로 변환합니다.
    MarketDetailResponse response = MarketDetailResponse.from(info);

    return CommonResponse.ok(response);
  }
}