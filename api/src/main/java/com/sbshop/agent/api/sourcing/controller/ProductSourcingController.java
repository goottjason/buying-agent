package com.sbshop.agent.api.sourcing.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.api.sourcing.dto.request.ProductSourcingRequest;
import com.sbshop.agent.api.sourcing.dto.response.ProductSourcingResponse;
import com.sbshop.agent.core.application.sourcing.usecase.ProductSourcingUseCase;
import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/sourcing")
@RequiredArgsConstructor
public class ProductSourcingController {

  private final ProductSourcingUseCase productSourcingUseCase;

  @PostMapping("/iherb")
  public CommonResponse<List<ProductSourcingResponse>> sourceFromIherb(
      @RequestBody ProductSourcingRequest request
  ) {
    log.info("🚀 아이허브 상품 수집 요청 - URL 개수: {}개", request.urls().size());

    // 1. UseCase로부터 순수 데이터(DTO) 리스트를 받습니다. (SourcedProduct 안 씀!)
    List<ScrapedProductDto> scrapedProducts = productSourcingUseCase.sourceFromIherb(request.urls());

    // 2. 컨트롤러에서 웹 응답용 DTO로 깔끔하게 변환(Mapping)합니다.
    List<ProductSourcingResponse> responses = scrapedProducts.stream()
        .map(ProductSourcingResponse::from)
        .toList();

    log.info("✅ [아이허브 크롤링 응답 완료] 총 {}건 변환 성공", responses.size());
    return CommonResponse.ok(responses);
  }
}