package com.sbshop.agent.infrastructure.client.cafe24.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.client.cafe24.config.Cafe24Properties;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24TokenManager {

  private final Cafe24Properties properties;
  private final RestClient restClient = RestClient.create();

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

      // 로그 전용: Instant를 다시 서울 시간대 문자열로 예쁘게 변환
      String kstTimeStr = this.tokenExpiresAt
          .atZone(ZoneId.of("Asia/Seoul"))
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

      log.info("✅ Cafe24 토큰 갱신 완료 (만료일시: {})", kstTimeStr);

    } catch (Exception e) {
      log.error("❌ Cafe24 토큰 갱신 실패. 리프레시 토큰이 만료되었거나 잘못되었습니다. (상세 내용은 위 로그 확인)");
    }
  }

  private void requestTokenToCafe24(String payload) throws Exception {
    String authHeader = "Basic " + Base64.getEncoder().encodeToString(
        (properties.getClientId() + ":" + properties.getClientSecret()).getBytes(StandardCharsets.UTF_8));

    try {
      // RestClient를 이용한 아주 깔끔한 HTTP 통신
      JsonNode response = restClient.post()
          .uri(properties.getApiUrl() + "/oauth/token")
          .header(HttpHeaders.AUTHORIZATION, authHeader)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(payload)
          .retrieve()
          .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, resp) -> {
              String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
              log.error("❌ Cafe24 API 호출 에러 응답: {}", errorBody);
              throw new RuntimeException("Cafe24 API Error: " + errorBody);
          })
          .body(JsonNode.class);

      if (response != null && response.has("access_token")) {
        this.accessToken = response.get("access_token").asText();
        
        // 날짜 파싱 개선: 공백이 있으면 T로 치환하여 ISO 형식으로 맞춤
        String expiresAtStr = response.get("expires_at").asText().replace(" ", "T");
        this.tokenExpiresAt = LocalDateTime.parse(expiresAtStr)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant();

        // 발급된 새 리프레시 토큰을 파일에 덮어씁니다.
        String newRefreshToken = response.get("refresh_token").asText();
        Files.writeString(new File(properties.getTokenPath()).toPath(), newRefreshToken);
      }
    } catch (Exception e) {
      throw e;
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

  // Cafe24TokenManager.java 내부에 아래 메서드를 추가합니다.

  /**
   * 브라우저에서 받아온 '인증 코드(code)'를 입력받아 최초의 토큰을 발급받고 파일에 저장합니다.
   */
  public void issueInitialToken(String code) {
    try {
      // 인가 코드로 최초 토큰 발급을 요청하는 Payload
      String payload = String.format("grant_type=authorization_code&code=%s&redirect_uri=%s",
          code, properties.getRedirectUri());

      // 기존에 만들어둔 찰떡같은 메서드 재활용! (여기서 파일 저장까지 다 해줍니다)
      requestTokenToCafe24(payload);

      log.info("🎉 [최초 인증 성공] 리프레시 토큰이 성공적으로 발급되어 파일에 저장되었습니다!");
    } catch (Exception e) {
      log.error("❌ 최초 인증 코드(code)로 토큰을 발급받는 데 실패했습니다.", e);
      throw new RuntimeException("최초 토큰 발급 실패", e);
    }
  }
}