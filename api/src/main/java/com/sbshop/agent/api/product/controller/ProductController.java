package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.api.product.dto.ImageUpdateRequest;
import com.sbshop.agent.api.product.dto.PriceStockUpdateRequest;
import com.sbshop.agent.api.product.dto.ProductDetailResponse;
import com.sbshop.agent.api.product.dto.ProductGridResponse;
import com.sbshop.agent.core.application.product.ProductManageUseCase;
import com.sbshop.agent.core.application.product.ProductSearchUseCase;
import com.sbshop.agent.core.application.product.dto.ProductMarketAggregate;
import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
   * 🚀 [수정] 상품 이미지 파일 업로드 및 상세 HTML 수정 + 마켓 브로드캐스트
   * 프론트엔드에서 'multipart/form-data' 형식으로 이미지 파일들을 보냅니다.
   */
  @PutMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CommonResponse<Void> updateImagesAndHtml(
      @PathVariable("id") Long id,
      @RequestPart(value = "images", required = false) List<MultipartFile> images // 🚀 실제 파일을 받습니다!
      // @RequestBody ImageUpdateRequest request
  ) {
    int imageCount = (images != null) ? images.size() : 0;
    log.info("상품 이미지/HTML 수정 및 동기화 요청 - ID: {}, 업로드 파일 수: {}", id, imageCount);

    if (images == null || images.isEmpty()) {
      throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
    }

    // 🚀 핵심: 프레임워크 객체(MultipartFile)를 순수 도메인 객체(ImageUploadFile)로 번역
    List<ImageUploadFile> uploadFiles = images.stream()
        .filter(file -> !file.isEmpty())
        .map(file -> {
          try {

            // 🚀 1. 압축된 이미지를 담을 메모리 스트림 준비
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            // 🚀 2. Thumbnailator를 이용한 이미지 최적화 마법!
            Thumbnails.of(file.getInputStream())
                .size(1000, 1000)    // 가로세로 최대 1000px로 비율 유지하며 축소
                .outputFormat("jpg") // 모든 이미지를 용량이 적은 jpg로 통일
                .outputQuality(0.8)  // 화질 80%로 압축 (육안으로 차이 없으나 용량은 반토막)
                .toOutputStream(os);
            // 3. 압축 완료된 바이트 배열 추출
            byte[] optimizedBytes = os.toByteArray();
            InputStream optimizedInputStream = new ByteArrayInputStream(optimizedBytes);

            // 4. 원본 파일명 추출 및 확장자를 .jpg로 강제 변경
            String originalName = file.getOriginalFilename();
            String baseName = (originalName != null && originalName.contains("."))
                ? originalName.substring(0, originalName.lastIndexOf("."))
                : "image";
            String optimizedFilename = baseName + ".jpg";

            // 5. 순수 도메인 객체(ImageUploadFile)로 포장해서 반환
            return new ImageUploadFile(
                optimizedFilename,
                "image/jpeg", // 강제로 jpg로 바꿨으니 Content-Type도 변경
                optimizedInputStream,
                optimizedBytes.length // 압축된 새로운 파일 용량
            );
          } catch (Exception e) {
            log.error("이미지 리사이징 중 오류 발생: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("이미지 최적화 처리 중 오류가 발생했습니다.", e);
          }
        })
        .toList();
    // UseCase 호출 (클라우드 업로드 -> detailHtml 및 hostedImages 치환 -> DB 업데이트 -> 마켓 반영)
    // 🚀 압축되어 가벼워진 순수 파일 객체들을 UseCase로 전달!
    productManageUseCase.updateAndBroadcastImagesAndHtml(id, uploadFiles);

    return CommonResponse.ok(null);
  }
}