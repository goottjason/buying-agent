package com.sbshop.agent.infrastructure.external.coupang.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "coupang")
public class CoupangProperties {
  private String vendorId;
  private String accessKey;
  private String secretKey;

  // 쿠팡 OpenAPI 기본 호스트 주소
  public String getApiUrl() {
    return "https://api-gateway.coupang.com";
  }
}