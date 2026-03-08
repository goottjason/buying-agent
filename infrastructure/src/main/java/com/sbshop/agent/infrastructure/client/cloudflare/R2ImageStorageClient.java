package com.sbshop.agent.infrastructure.client.cloudflare;

import com.sbshop.agent.core.domain.product.client.ImageStorageClient;
import com.sbshop.agent.infrastructure.client.cloudflare.config.R2Properties;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class R2ImageStorageClient implements ImageStorageClient {

  private final R2Properties r2Properties;
  // 실제 S3Client 주입 (Config 파일에서 빈으로 등록했다고 가정)
  private final S3Client s3Client;

  @Override
  public Map<String, String> uploadImages(List<String> sourceImages) {
    Map<String, String> uploadedUrlMap = new LinkedHashMap<>();

    for (String sourcePath : sourceImages) {
      // 1. 파일 이름 난수화 (덮어쓰기 방지)
      String extension = ".jpg"; // 실제로는 sourcePath에서 확장자 추출
      String fileName = UUID.randomUUID().toString() + extension;

      try {
        // ==========================================================
        // 🚀 실제 S3 업로드 로직 (주석 해제 후 사용)
        // ==========================================================
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(r2Properties.getBucket())
            .key(fileName)
            .contentType("image/jpeg")
            .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(new File(sourcePath)));

        // 2. 업로드 성공 시 Public URL 조립
        String publicUrl = r2Properties.getPublicUrl() + "/" + fileName;
        uploadedUrlMap.put(sourcePath, publicUrl);

        log.info("R2 업로드 성공: {} -> {}", sourcePath, publicUrl);

      } catch (Exception e) {
        log.error("R2 업로드 실패: {}", sourcePath, e);
        // 실패 처리 로직 (스킵하거나 예외 던지기)
      }
    }
    return uploadedUrlMap;
  }
}