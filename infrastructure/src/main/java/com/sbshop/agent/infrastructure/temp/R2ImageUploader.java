/*
package com.sbshop.agent.infrastructure.client.cloudflare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2ImageUploader {

  private final S3Client s3Client;

  @Value("${cloud.cloudflare.r2.bucket}")
  private String bucket;

  @Value("${cloud.cloudflare.r2.public-url}")
  private String publicUrl;

  */
/**
   * 이미지를 R2에 업로드하고, 전 세계 어디서든 볼 수 있는 퍼블릭 URL을 반환합니다.
   * @param file 화면에서 넘어온 이미지 파일
   * @param sku 상품 자체 SKU (예: 210121IHB036)
   * @param index 이미지 순번 (예: 1, 2, 3...)
   *//*

  public String uploadImage(MultipartFile file, String sku, int index) {
    try {
      // 🚀 1. 저장될 파일 이름 조립 (예: products/210121IHB036/210121IHB036-1.jpg)
      // 폴더 구조를 깔끔하게 가져가기 위해 SKU 폴더 안에 넣습니다.
      String objectKey = String.format("products/%s/%s-%d.jpg", sku, sku, index);

      // 🚀 2. R2에 업로드할 요청 폼 만들기
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .contentType(file.getContentType()) // "image/jpeg" 등 인식용
          .build();

      // 🚀 3. 파일 쏘아올리기!
      s3Client.putObject(putObjectRequest,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      // 🚀 4. 퍼블릭 URL 조립해서 반환
      String finalUrl = publicUrl + "/" + objectKey;
      log.info("✅ [R2 업로드 성공] 파일 URL: {}", finalUrl);

      return finalUrl;

    } catch (IOException e) {
      log.error("❌ 이미지 파일 스트림 읽기 실패: {}", e.getMessage());
      throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
    } catch (Exception e) {
      log.error("❌ R2 클라우드 업로드 실패: {}", e.getMessage());
      throw new RuntimeException("클라우드 스토리지 연동 오류입니다.", e);
    }
  }
}*/
