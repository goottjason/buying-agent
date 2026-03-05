package com.sbshop.agent.core.domain.product.service;

import com.sbshop.agent.core.domain.market.component.MarketSyncOrchestrator;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ImageUploadData;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.port.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageUpdateService {

  // 🚀 구현체(R2) 대신 인터페이스(Port)를 주입받습니다!
  private final ImageStoragePort imageStoragePort;
  private final ProductFinder productFinder;
  private final MarketSyncOrchestrator syncOrchestrator;

  @Transactional
  public void updateImagesAndSync(Long productId, ImageUploadData repImage, List<ImageUploadData> detailImages) {
    Product product = productFinder.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    String sku = product.getSku();

    List<String> newHostedImages = new ArrayList<>();

    // 1. 대표 이미지 처리
    if (repImage != null) {
      String uploadedRepUrl = imageStoragePort.uploadImage(repImage, sku, 0);
      newHostedImages.add(uploadedRepUrl);
    } else {
      newHostedImages.add(product.getRepImageUrl());
    }

    // 2. 상세 이미지 처리
    if (detailImages != null && !detailImages.isEmpty()) {
      for (int i = 0; i < detailImages.size(); i++) {
        String url = imageStoragePort.uploadImage(detailImages.get(i), sku, i + 1);
        newHostedImages.add(url);
      }
    } else {
      List<String> existingImages = product.getHostedImages();
      if (existingImages.size() > 1) {
        newHostedImages.addAll(existingImages.subList(1, existingImages.size()));
      }
    }

    // 3. HTML 생성
    List<String> detailUrlsOnly = newHostedImages.size() > 1
        ? newHostedImages.subList(1, newHostedImages.size())
        : new ArrayList<>();
    String newDetailHtml = generateNewDetailHtml(detailUrlsOnly, product.getDetailHtml());

    // =====================================================================
    // 🚀 [수정] 엔티티를 건드리지 않고, 기존에 만들어둔 Command 객체를 활용합니다!
    // =====================================================================
    ProductUpdateCommand command = ProductUpdateCommand.builder()
        .hostedImages(newHostedImages) // ImageInfo VO 업데이트 트리거
        .detailHtml(newDetailHtml)     // Flat 필드 업데이트 트리거
        .build();

    // 완성된 완전체 update 메서드 호출!
    product.update(command);
    // =====================================================================

    log.info("로컬 Product 마스터(SKU: {}) 이미지 및 HTML 갱신 완료!", sku);

    // 4. 마켓 동기화 지휘관 호출
    syncOrchestrator.asyncUpdateMarketImages(product.getId());
  }

  /**
   * [내부 헬퍼] 새로운 이미지 URL 리스트로 깔끔한 HTML을 재생성합니다.
   */
  private String generateNewDetailHtml(List<String> newUrls, String oldHtml) {
    if (newUrls.isEmpty()) return oldHtml;

    StringBuilder htmlBuilder = new StringBuilder();
    htmlBuilder.append("<div style=\"text-align: center;\">\n");

    for (String url : newUrls) {
      htmlBuilder.append(String.format("<img src=\"%s\" style=\"max-width: 100%%; margin: 10px auto; display: block;\" /><br/>\n", url));
    }

    htmlBuilder.append("</div>");
    return htmlBuilder.toString();
  }
}