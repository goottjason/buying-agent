package com.sbshop.agent.infrastructure.external.cloudflare.adapter;

import com.sbshop.agent.core.domain.product.dto.ImageUploadData;
import com.sbshop.agent.core.domain.product.port.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class R2ImageStorageAdapter implements ImageStoragePort {

  private final S3Client s3Client;

  @Value("${cloud.cloudflare.r2.bucket}")
  private String bucket;

  @Value("${cloud.cloudflare.r2.public-url}")
  private String publicUrl;

  @Override
  public String uploadImage(ImageUploadData fileData, String sku, int index) {
    try {
      String objectKey = String.format("products/%s/%s-%d.jpg", sku, sku, index);

      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .contentType(fileData.contentType()) // DTO에서 꺼내 씀
          .build();

      // DTO의 InputStream을 활용하여 SDK에 던짐
      s3Client.putObject(putObjectRequest,
          RequestBody.fromInputStream(fileData.inputStream(), fileData.size()));

      return publicUrl + "/" + objectKey;

    } catch (Exception e) {
      log.error("❌ R2 클라우드 업로드 실패: {}", e.getMessage());
      throw new RuntimeException("클라우드 스토리지 연동 오류입니다.", e);
    }
  }
}