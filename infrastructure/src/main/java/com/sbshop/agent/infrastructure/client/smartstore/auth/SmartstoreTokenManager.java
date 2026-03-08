package com.sbshop.agent.infrastructure.client.smartstore.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.client.smartstore.config.SmartstoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreTokenManager {

  private final SmartstoreProperties properties;
  private final RestClient restClient = RestClient.create();

  private String accessToken;
  private Instant tokenExpiresAt;

  /**
   * 외부에서 스마트스토어 API를 호출할 때 무조건 이 메서드로 토큰을 가져갑니다.
   */
  public synchronized String getValidAccessToken() {
    // 만료 5분 전이거나 토큰이 없으면 새로 발급
    if (accessToken == null || tokenExpiresAt == null || tokenExpiresAt.minusSeconds(300).isBefore(Instant.now())) {
      issueNewToken();
    }
    return accessToken;
  }

  private void issueNewToken() {
    try {
      long timestamp = System.currentTimeMillis();

      // 1. BCrypt 암호화 서명 생성 (네이버 공식 스펙)
      String password = properties.getClientId() + "_" + timestamp;
      String hashedPw = BCrypt.hashpw(password, properties.getClientSecret());
      String clientSecretSign = Base64.getEncoder().encodeToString(hashedPw.getBytes(StandardCharsets.UTF_8));

      // 2. form-urlencoded 바디 생성
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "client_credentials");
      body.add("client_id", properties.getClientId());
      body.add("timestamp", String.valueOf(timestamp));
      body.add("client_secret_sign", clientSecretSign);
      body.add("type", "SELF");

      // 3. 깔끔한 RestClient 통신
      JsonNode response = restClient.post()
          .uri(properties.getApiUrl() + "/v1/oauth2/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(body)
          .retrieve()
          .body(JsonNode.class);

      if (response != null && response.has("access_token")) {
        this.accessToken = response.get("access_token").asText();
        // 네이버는 보통 expires_in (초 단위, 약 10800초=3시간)을 내려줍니다.
        long expiresInSeconds = response.get("expires_in").asLong();
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds);

        log.info("✅ 스마트스토어 토큰 갱신 완료 (만료 예정: {})", this.tokenExpiresAt);
      }
    } catch (Exception e) {
      log.error("❌ 스마트스토어 토큰 발급 실패: {}", e.getMessage(), e);
      throw new RuntimeException("스마트스토어 토큰 발급 실패");
    }
  }
}