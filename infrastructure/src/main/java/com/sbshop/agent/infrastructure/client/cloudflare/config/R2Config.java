package com.sbshop.agent.infrastructure.client.cloudflare.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class R2Config {

  private final R2Properties r2Properties;

  @Bean
  public S3Client s3Client() {
    // 1. YAML에서 가져온 액세스 키와 시크릿 키로 인증 객체 생성
    AwsBasicCredentials credentials = AwsBasicCredentials.create(
        r2Properties.getAccessKey(),
        r2Properties.getSecretKey()
    );

    // 2. S3Client 객체를 조립하여 스프링 빈으로 등록
    return S3Client.builder()
        .endpointOverride(URI.create(r2Properties.getEndpoint())) // R2 엔드포인트
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of("auto")) // 클라우드플레어 R2는 리전을 'auto'로 설정하는 것이 표준입니다.
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true) // R2 및 호환 스토리지를 위해 필수적인 옵션
            .build())
        .build();
  }
}