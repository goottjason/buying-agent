package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.domain.product.client.ImageDownloadClient;
import com.sbshop.agent.core.domain.product.client.ImageStorageClient;
import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.component.ProductWriter;
import com.sbshop.agent.core.domain.product.dto.ProductCreateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.model.enums.CategoryType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCreateUseCase {

  private final ProductWriter productWriter;
  private final ProductReader productReader;
  private final ImageStorageClient imageStorageClient;
  private final ImageDownloadClient imageDownloadClient;

  @Transactional
  public void createBulk(List<ProductCreateCommand> baseCommands) {

    // 1. SKU Prefix 생성 (예: "20260315IHB")
    String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String skuPrefix = datePrefix + "IHB";

    // 2. DB에서 오늘자 마지막 시퀀스 번호 조회
    // (Reader 내부에 SELECT MAX(sku) ... LIKE '20260315IHB%' 로직이 있다고 가정)
    int currentSequence = productReader.getNextSkuSequence(skuPrefix);

    List<Product> productsToSave = new ArrayList<>();

    for (ProductCreateCommand command : baseCommands) {

      // 3. SKU 채번
      String generatedSku = String.format("%sIHB%03d", skuPrefix, currentSequence++);

      // 4. 이미지 업로드 파이프라인
      List<ImageUploadFile> uploadFiles = imageDownloadClient.downloadAll(command.sourceImages());
      Map<String, String> uploadedUrlMap = imageStorageClient.uploadImages(uploadFiles);
      List<String> hostedImages = new ArrayList<>(uploadedUrlMap.values());

      // 5. 호스팅된 이미지 주소만 덧붙이기
      ProductCreateCommand enrichedCommand = command.toBuilder()
          .hostedImages(hostedImages)
          .build();

      // 6. 엔티티 생성
      Product product = Product.create(generatedSku, enrichedCommand);
      productsToSave.add(product);
    }

    // 6. 로컬 DB 일괄 저장
    productWriter.writeAll(productsToSave);
    log.info("✅ 총 {}개의 상품이 로컬 DB에 성공적으로 저장되었습니다.", productsToSave.size());
  }
}