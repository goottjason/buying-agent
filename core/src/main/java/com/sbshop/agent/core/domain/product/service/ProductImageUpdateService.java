package com.sbshop.agent.core.domain.product.service;

import com.sbshop.agent.core.domain.market.component.MarketSyncOrchestrator;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.dto.ImageUploadData;
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
  // 🚀 MultipartFile 대신 순수 자바 DTO인 ImageUploadData를 받습니다.
  public void updateImagesAndSync(Long productId, ImageUploadData repImage, List<ImageUploadData> detailImages) {
    Product product = productFinder.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    String sku = product.getSku();

    String newRepImageUrl = product.getRepImageUrl();
    List<String> newDetailUrls = new ArrayList<>();

    // 🚀 Port를 통해 업로드 (인프라가 뭔지 몰라도 됨)
    if (repImage != null) {
      newRepImageUrl = imageStoragePort.uploadImage(repImage, sku, 0);
      log.info("대표 이미지 업로드 완료: {}", newRepImageUrl);
    }

    if (detailImages != null && !detailImages.isEmpty()) {
      for (int i = 0; i < detailImages.size(); i++) {
        String url = imageStoragePort.uploadImage(detailImages.get(i), sku, i + 1);
        newDetailUrls.add(url);
      }
    }

    String newDetailHtml = generateNewDetailHtml(newDetailUrls, product.getDetailHtml());
    product.updateImages(newRepImageUrl, newDetailUrls, newDetailHtml);
    log.info("로컬 Product 마스터(SKU: {}) 갱신 완료!", sku);

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