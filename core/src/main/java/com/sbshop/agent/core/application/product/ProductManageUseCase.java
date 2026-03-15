package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import com.sbshop.agent.core.domain.market.component.MarketRegistrationReader;
import com.sbshop.agent.core.domain.market.dto.MarketRegistrationUpdateCommand;
import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.MarketClientRouter;
import com.sbshop.agent.core.domain.product.client.ImageStorageClient;
import com.sbshop.agent.core.domain.product.component.HtmlImageReplacer;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.dto.ProductUpdateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductManageUseCase {

  private final ProductReader productReader;
  private final MarketRegistrationReader registrationReader;
  private final MarketClientRouter clientRouter;

  private final HtmlImageReplacer htmlImageReplacer;
  private final ImageStorageClient imageStorageClient;

  /**
   * 1단계: 로컬 상품의 가격/재고를 수정하고, 연결된 모든 마켓에 동기화합니다.
   */
  @Transactional
  public void updateAndBroadcastPriceStock(Long productId, Integer newPrice, Integer newStock) {
    // =====================================================================
    // 1. 로컬 Product 업데이트 (만능 Command 사용!)
    // =====================================================================
    // 1. 로컬 DB 상품 수정 (JPA 더티 체킹으로 자동 UPDATE)
    Product product = productReader.read(productId);

    ProductUpdateCommand productCommand = ProductUpdateCommand.builder()
        // (참고: 기존에 BigDecimal로 설계하셨다면 BigDecimal.valueOf(newPrice) 등 활용)
        .salePrice(newPrice != null ? BigDecimal.valueOf(newPrice) : null)
        .stock(newStock)
        .build();
    product.update(productCommand);
    log.info("로컬 상품(ID:{}) 가격/재고 업데이트 완료", productId);

    // =====================================================================
    // 2. 마켓 브로드캐스트 및 MarketRegistration 업데이트
    // =====================================================================
    // 2. 이 상품이 등록된 모든 마켓 연동 기록을 가져옵니다.
    List<MarketRegistration> registrations = registrationReader.readAllByProductId(productId);

    // 3. 브로드캐스트 (전파)!
    for (MarketRegistration reg : registrations) {
      try {
        // 해당 마켓의 무전기(Client)를 찾아서 쏜다!
        MarketClient client = clientRouter.getClient(reg.getMarketType());
        // 🚀 API 전송 성공 시, 해당 마켓 규칙에 맞게 수정된(Patched) Map 데이터를 돌려받음!
        Map<String, Object> patchedData = client.syncPriceAndStock(
            reg.getMarketItemId(),
            reg.getMarketDetailedInfo(), // 기존 데이터를 던져줌
            newPrice,
            newStock
        );

        // 🚀 만능 Command를 사용하여 MarketRegistration의 JSON 데이터와 동기화 시간 갱신!
        MarketRegistrationUpdateCommand regCommand = MarketRegistrationUpdateCommand.builder()
            .marketDetailedInfo(patchedData)
            .isSynced(true)
            .lastSyncedAt(LocalDateTime.now())
            .build();
        reg.update(regCommand);
        log.info("마켓({}) 동기화 성공 - 아이템ID: {}", reg.getMarketType(), reg.getMarketItemId());
      } catch (Exception e) {
        // 💡 하나의 마켓이 뻗어도 다른 마켓 업데이트는 계속 진행되어야 하므로 try-catch로 방어!
        log.error("마켓({}) 동기화 실패 - 아이템ID: {}", reg.getMarketType(), reg.getMarketItemId(), e);
        reg.update(MarketRegistrationUpdateCommand.builder()
            .isSynced(false)
            .build());
      }
    }
  }

  @Transactional
  public void updateAndBroadcastImagesAndHtml(Long productId, List<ImageUploadFile> images) {
    // 1. 상품 엔티티 조회
    Product product = productReader.read(productId);

    // 마켓으로 쏠 최종 데이터 변수 선언
    List<String> finalHostedImages;
    String finalDetailHtml;

    // =====================================================================
    // 🚀 [핵심 분기] 새로 넘어온 이미지가 있는지 확인합니다.
    // =====================================================================
    if (images != null && !images.isEmpty()) {
      // [Case A] 새 이미지가 들어온 경우: 클라우드 업로드 -> HTML 치환 -> 로컬 DB 갱신
      Map<String, String> uploadedUrlMap = imageStorageClient.uploadImages(images);
      finalHostedImages = uploadedUrlMap.values().stream().toList();

      String oldHtml = product.getDetailHtml();
      String sku = product.getSku();
      finalDetailHtml = htmlImageReplacer.replaceImagesBySku(oldHtml, sku, finalHostedImages);

      // 로컬 Product 엔티티 업데이트
      ProductUpdateCommand productCommand = ProductUpdateCommand.builder()
          .hostedImages(finalHostedImages)
          .detailHtml(finalDetailHtml)
          .build();
      product.update(productCommand);
      log.info("로컬 상품(ID:{}) 이미지 및 HTML 업데이트 완료", productId);

    } else {
      // [Case B] 새 이미지가 없는 경우 (강제 동기화): 로컬 DB의 기존 값 재사용
      // (도메인 구조에 따라 product.getImageInfo().getHostedImages() 일 수 있으니 getter 확인 필요)
      finalHostedImages = product.getHostedImages();
      finalDetailHtml = product.getDetailHtml();

      // 방어 로직: 기존 DB에도 전송할 이미지가 아예 없다면 막아줍니다.
      if (finalHostedImages == null || finalHostedImages.isEmpty()) {
        throw new IllegalStateException("동기화할 기존 이미지 정보가 DB에 존재하지 않습니다.");
      }
      log.info("새로운 이미지 업로드 없이 기존 로컬 DB 데이터로 마켓 강제 동기화를 진행합니다. (상품 ID: {})", productId);
    }

    // =====================================================================
    // 4. 마켓 브로드캐스트 및 MarketRegistration 업데이트 (공통 로직)
    // =====================================================================
    List<MarketRegistration> registrations = registrationReader.readAllByProductId(productId);

    for (MarketRegistration reg : registrations) {
      try {
        MarketClient client = clientRouter.getClient(reg.getMarketType());

        // 🚀 분기 처리를 통해 결정된 최종 이미지와 HTML을 마켓에 쏩니다!
        Map<String, Object> patchedData = client.syncImagesAndHtml(
            reg.getMarketItemId(),
            reg.getMarketDetailedInfo(),
            finalHostedImages,
            finalDetailHtml
        );

        MarketRegistrationUpdateCommand regCommand = MarketRegistrationUpdateCommand.builder()
            .marketDetailedInfo(patchedData)
            .isSynced(true)
            .lastSyncedAt(LocalDateTime.now())
            .build();

        reg.update(regCommand);
        log.info("마켓({}) 이미지/HTML 동기화 성공", reg.getMarketType());

      } catch (Exception e) {
        log.error("마켓({}) 이미지/HTML 동기화 실패", reg.getMarketType(), e);
        reg.update(MarketRegistrationUpdateCommand.builder()
            .isSynced(false)
            .build());
      }
    }
  }

  /*@Transactional
  public void updateAndBroadcastImagesAndHtml(Long productId, List<ImageUploadFile> images) {
    // 클라우드 업로드 -> detailHtml 및 hostedImages 치환 -> DB 업데이트 -> 마켓 반영
    Product product = productReader.read(productId);


    // =====================================================================
    // 1. Cloudflare R2에 이미지 업로드 및 URL 매핑 생성
    // =====================================================================
    // (예: imageStorageClient가 업로드를 수행하고 { "옛날URL" : "새로운R2_URL" } Map을 반환한다고 가정)
    Map<String, String> uploadedUrlMap = imageStorageClient.uploadImages(images);

    // Map에서 Value(새로운 주소)만 쫙 뽑아서 List로 만듭니다.
    List<String> hostedImages = uploadedUrlMap.values().stream().toList();

    // =====================================================================
    // 2. HTML 치환 (기존 이미지 주소를 R2 주소로 교체)
    // =====================================================================
    String oldHtml = product.getDetailHtml();
    String sku = product.getSku(); // "210909FM032"
    String newDetailHtml = htmlImageReplacer.replaceImagesBySku(oldHtml, sku, hostedImages);

    // =====================================================================
    // 3. 로컬 Product 엔티티 업데이트 (만능 Command!)
    // =====================================================================
    ProductUpdateCommand productCommand = ProductUpdateCommand.builder()
        .hostedImages(hostedImages) // 🚀 2번 요구사항 (hostedImages 변경)
        .detailHtml(newDetailHtml)  // 🚀 3번 요구사항 (detailHtml 변경)
        .build();

    product.update(productCommand);
    log.info("로컬 상품(ID:{}) 이미지 및 HTML 업데이트 완료", productId);

    // =====================================================================
    // 4. 마켓 브로드캐스트 및 MarketRegistration 업데이트
    // =====================================================================
    List<MarketRegistration> registrations = registrationReader.readAllByProductId(productId);

    for (MarketRegistration reg : registrations) {
      try {
        MarketClient client = clientRouter.getClient(reg.getMarketType());

        // 🚀 각 마켓에 새 이미지 리스트와 HTML을 쏘고, 패치된 Map 데이터를 받음!
        Map<String, Object> patchedData = client.syncImagesAndHtml(
            reg.getMarketItemId(),
            reg.getMarketDetailedInfo(),
            hostedImages,
            newDetailHtml
        );

        // 🚀 만능 Command를 사용하여 MarketRegistration 갱신
        MarketRegistrationUpdateCommand regCommand = MarketRegistrationUpdateCommand.builder()
            .marketDetailedInfo(patchedData)
            .isSynced(true)
            .lastSyncedAt(LocalDateTime.now())
            .build();

        reg.update(regCommand);
        log.info("마켓({}) 이미지/HTML 동기화 성공", reg.getMarketType());

      } catch (Exception e) {
        log.error("마켓({}) 이미지/HTML 동기화 실패", reg.getMarketType(), e);
        reg.update(MarketRegistrationUpdateCommand.builder()
            .isSynced(false)
            .build());
      }
    }
  }*/
}