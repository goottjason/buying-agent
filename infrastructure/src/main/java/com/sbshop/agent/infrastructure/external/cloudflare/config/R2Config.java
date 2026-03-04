package com.sbshop.agent.infrastructure.external.cloudflare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {

  @Value("${cloud.cloudflare.r2.endpoint}")
  private String endpoint;

  @Value("${cloud.cloudflare.r2.access-key}")
  private String accessKey;

  @Value("${cloud.cloudflare.r2.secret-key}")
  private String secretKey;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointOverride(URI.create(endpoint))
        // R2는 리전 제약이 없어서 보통 "auto" 또는 "us-east-1"을 씁니다.
        .region(Region.of("auto"))
        .build();
  }
}