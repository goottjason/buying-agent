package com.sbshop.agent.infrastructure.cafe24.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.cafe24.config.Cafe24Properties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24TokenManager {

  private final Cafe24Properties properties;
  private final RestClient restClient = RestClient.create(); // 최신 HTTP 클라이언트

  private String accessToken;
  private Instant tokenExpiresAt;

  @PostConstruct
  public void init() {
    File tokenFile = new File(properties.getTokenPath());
    if (!tokenFile.exists()) {
      log.warn("🚨 Cafe24 인증이 필요합니다! 브라우저에서 아래 URL로 접속해 인증 코드를 받아주세요.");
      log.warn(generateAuthorizationUrl());
    } else {
      refreshAccessToken(); // 서버 켜질 때 기존 리프레시 토큰으로 자동 갱신
    }
  }

  /**
   * 외부(WebClient)에서 API 쏠 때 이 메서드로 항상 유효한 엑세스 토큰을 받아갑니다.
   */
  public synchronized String getValidAccessToken() {
    // 만료 5분 전이면 안전하게 갱신
    if (accessToken == null || tokenExpiresAt == null || tokenExpiresAt.minusSeconds(300).isBefore(Instant.now())) {
      refreshAccessToken();
    }
    return accessToken;
  }

  private void refreshAccessToken() {
    try {
      String refreshToken = readRefreshTokenFromFile();
      if (refreshToken == null || refreshToken.isBlank()) return;

      String payload = "grant_type=refresh_token&refresh_token=" + refreshToken;
      requestTokenToCafe24(payload);

      log.info("✅ Cafe24 토큰 갱신 완료 (만료일시: {})", tokenExpiresAt);

    } catch (Exception e) {
      log.error("❌ Cafe24 토큰 갱신 실패. 리프레시 토큰이 만료되었거나 잘못되었습니다.", e);
    }
  }

  private void requestTokenToCafe24(String payload) throws Exception {
    String authHeader = "Basic " + Base64.getEncoder().encodeToString(
        (properties.getClientId() + ":" + properties.getClientSecret()).getBytes(StandardCharsets.UTF_8));

    // RestClient를 이용한 아주 깔끔한 HTTP 통신
    JsonNode response = restClient.post()
        .uri(properties.getApiUrl() + "/oauth/token")
        .header(HttpHeaders.AUTHORIZATION, authHeader)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(payload)
        .retrieve()
        .body(JsonNode.class);

    if (response != null) {
      this.accessToken = response.get("access_token").asText();
      this.tokenExpiresAt = Instant.parse(response.get("expires_at").asText() + "Z");

      // 발급된 새 리프레시 토큰을 파일에 덮어씁니다.
      String newRefreshToken = response.get("refresh_token").asText();
      Files.writeString(new File(properties.getTokenPath()).toPath(), newRefreshToken);
    }
  }

  private String readRefreshTokenFromFile() throws Exception {
    File file = new File(properties.getTokenPath());
    if (!file.exists()) return null;
    return Files.readString(file.toPath()).trim();
  }

  private String generateAuthorizationUrl() {
    return String.format(
        "%s/oauth/authorize?response_type=code&client_id=%s&state=shouldbeshopping&redirect_uri=%s&scope=%s",
        properties.getApiUrl(), properties.getClientId(), properties.getRedirectUri(), properties.getScope()
    );
  }
}