/*
package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.core.domain.product.dto.ImageUploadData;
import com.sbshop.agent.core.domain.product.service.ProductImageUpdateService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductImageController {

  private final ProductImageUpdateService imageUpdateService;

  @PostMapping("/{productId}/images")
  public ResponseEntity<CommonResponse<Void>> updateProductImages(
      @PathVariable Long productId,
      @RequestParam(value = "repImage", required = false) MultipartFile repImage,
      @RequestParam(value = "detailImages", required = false) List<MultipartFile> detailImages
  ) throws IOException { // 간단한 예외 처리 추가

    // 🚀 1. MultipartFile -> 순수 DTO 변환
    ImageUploadData repImageData = (repImage != null && !repImage.isEmpty())
        ? convertToDto(repImage) : null;

    List<ImageUploadData> detailImageDataList = new ArrayList<>();
    if (detailImages != null) {
      for (MultipartFile file : detailImages) {
        if (!file.isEmpty()) detailImageDataList.add(convertToDto(file));
      }
    }

    // 🚀 2. Core Service 호출 (Web 의존성 완벽 차단!)
    imageUpdateService.updateImagesAndSync(productId, repImageData, detailImageDataList);

    return ResponseEntity.ok(CommonResponse.ok());
  }

  // 헬퍼 메서드
  private ImageUploadData convertToDto(MultipartFile file) throws IOException {
    return new ImageUploadData(
        file.getInputStream(),
        file.getOriginalFilename(),
        file.getContentType(),
        file.getSize()
    );
  }
}*/
