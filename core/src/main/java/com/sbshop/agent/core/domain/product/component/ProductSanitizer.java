package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.sourcing.component.SourcingAgentFactory;
import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.product.client.ImageDownloadClient;
import com.sbshop.agent.core.domain.product.client.ImageStorageClient;
import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand; // 💡 종원님의 완벽한 Command 객체
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSanitizer {

  private final SourcingAgentFactory agentFactory;
  private final ImageDownloadClient imageDownloadClient;
  private final ImageStorageClient imageStorageClient;

  /**
   * 🚀 마켓 전송 전, 상품 데이터를 최신 규격으로 강제 정제합니다.
   */
  public void sanitizeForPublish(Product product) {
    cleanUpLegacyImages(product);
  }

  private void cleanUpLegacyImages(Product product) {
    List<String> hostedImages = product.getImageInfo() != null
        ? product.getImageInfo().getHostedImages() : new ArrayList<>();

    // 1. ESM 호스팅 등 레거시 이미지가 포함되어 있는지 검사
    boolean hasLegacyImage = hostedImages.stream()
        .anyMatch(url -> url != null && url.contains("esmplus.com"));

    if (!hasLegacyImage) {
      return; // 이미 클라우드(최신) 이미지면 패스!
    }

    log.info("🧹 [마이그레이션] 레거시 이미지 감지됨. 이미지 최신화 파이프라인을 가동합니다 - SKU: {}", product.getSku());

    String sourceUrl = product.getSourcingInfo() != null ? product.getSourcingInfo().getSourceUrl() : null;

    // 💡 헷갈렸던 부분 수정: originalImages(X) -> sourceImages(O)
    List<String> targetSourceImages = product.getImageInfo() != null
        ? product.getImageInfo().getSourceImages() : new ArrayList<>();

    // 2. 아이허브 상품인 경우: 실시간 스크래핑으로 최신 sourceImages 긁어오기
    if (sourceUrl != null && sourceUrl.contains("iherb.com")) {
      log.info("🌿 [마이그레이션] 아이허브 상품 확인. 최신 이미지를 스크래핑합니다...");
      try {
        ScrapedProductInfo scrapedInfo = agentFactory.getAgentByUrl(sourceUrl).scrapeProduct(sourceUrl, null);
        targetSourceImages = scrapedInfo.getAdditionalImageUrls(); // 메인 이미지는 따로 처리하거나 함께 포함
      } catch (Exception e) {
        log.warn("⚠️ 아이허브 스크래핑 실패. 기존 DB의 sourceImages를 대체 사용합니다: {}", e.getMessage());
      }
    } else {
      log.info("🔄 [마이그레이션] 기존 DB의 sourceImages를 기반으로 재업로드를 준비합니다.");
    }

    // 3. 추출한 sourceImages가 존재하면 다운로드 & 클라우드 업로드 진행
    if (targetSourceImages != null && !targetSourceImages.isEmpty()) {
      log.info("☁️ [마이그레이션] 이미지 다운로드 및 클라우드 업로드 시작...");

      List<ImageUploadFile> uploadFiles = imageDownloadClient.downloadAll(targetSourceImages);
      Map<String, String> uploadedUrlMap = imageStorageClient.uploadImages(uploadFiles);
      List<String> newHostedImages = new ArrayList<>(uploadedUrlMap.values());

      // 🚀 4. 종원님의 '완벽한 update 메서드 (Command 패턴)' 적용!
      // (빌더 필드명은 종원님의 실제 Command 객체 스펙에 맞게 살짝 수정해서 써주세요)
      ProductUpdateCommand command = ProductUpdateCommand.builder()
          .sourceImages(targetSourceImages)
          .hostedImages(newHostedImages)
          .build();

      // 엔티티 업데이트 (트랜잭션 종료 시 JPA 더티체킹으로 자동 DB 반영)
      product.update(command);

      log.info("✅ [마이그레이션] 총 {}장의 이미지가 새 클라우드로 교체되었습니다!", newHostedImages.size());
    } else {
      throw new RuntimeException("sourceImages를 찾을 수 없어 마이그레이션에 실패했습니다.");
    }
  }
}