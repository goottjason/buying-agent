package com.sbshop.agent.infrastructure.client.cafe24.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cafe24")
public class Cafe24Properties {
  private String mallId;
  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String scope;
  private String tokenPath;

  public String getApiUrl() {
    return "https://" + mallId + ".cafe24api.com/api/v2";
  }
}