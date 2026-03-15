package com.sbshop.agent.infrastructure.client.image;

import com.sbshop.agent.core.domain.product.client.ImageDownloadClient;
import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDownloader implements ImageDownloadClient {

  // Spring Boot에서 기본 제공하는 HTTP 클라이언트
  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * 여러 개의 이미지 URL을 한 번에 다운로드합니다.
   */
  @Override
  public List<ImageUploadFile> downloadAll(List<String> imageUrls) {
    return imageUrls.stream()
        .filter(url -> url != null && !url.isBlank())
        .map(this::download)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * 단일 이미지 URL을 다운로드하여 ImageUploadFile 객체로 변환합니다.
   */
  @Override
  public Optional<ImageUploadFile> download(String imageUrl) {
    try {
      log.info("⬇️ 원본 이미지 다운로드 중: {}", imageUrl);

      // 1. URL에서 파일 다운로드 (byte 배열로 메모리에 적재)
      ResponseEntity<byte[]> response = restTemplate.exchange(
          URI.create(imageUrl),
          HttpMethod.GET,
          null,
          byte[].class
      );

      byte[] imageBytes = response.getBody();
      if (imageBytes == null || imageBytes.length == 0) {
        log.warn("다운로드 실패: 빈 이미지 데이터 ({})", imageUrl);
        return Optional.empty();
      }

      // 2. Content-Type 추출 (예: "image/jpeg")
      String contentType = Objects.requireNonNull(response.getHeaders().getContentType()).toString();

      // 3. 파일명 추출 (URL의 마지막 슬래시 뒷부분, 쿼리파라미터 제거)
      String filename = extractFilenameFromUrl(imageUrl);

      // 4. 종원님의 규격에 맞게 ImageUploadFile 레코드 조립
      ImageUploadFile uploadFile = new ImageUploadFile(
          filename,
          contentType,
          new ByteArrayInputStream(imageBytes), // 🚀 메모리의 byte[]를 안전한 스트림으로 변환
          imageBytes.length
      );

      return Optional.of(uploadFile);

    } catch (Exception e) {
      log.error("❌ 이미지 다운로드 중 오류 발생: {}", imageUrl, e);
      return Optional.empty(); // 에러 발생 시 해당 이미지는 스킵
    }
  }

  /**
   * URL에서 순수 파일명만 추출하는 유틸리티 메서드
   * 예: "https://.../image.jpg?v=123" -> "image.jpg"
   */
  private String extractFilenameFromUrl(String url) {
    String path = url;
    int queryIndex = path.indexOf('?');
    if (queryIndex != -1) {
      path = path.substring(0, queryIndex); // 쿼리 파라미터 날리기
    }
    int lastSlashIndex = path.lastIndexOf('/');
    if (lastSlashIndex != -1) {
      return path.substring(lastSlashIndex + 1);
    }
    return "unknown_image.jpg"; // 파싱 실패 시 기본값
  }
}