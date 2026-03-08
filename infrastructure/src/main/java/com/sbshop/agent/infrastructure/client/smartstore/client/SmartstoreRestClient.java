package com.sbshop.agent.infrastructure.client.smartstore.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.infrastructure.client.smartstore.config.SmartstoreProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreRestClient {

  private final SmartstoreProperties properties;
  private final RestClient restClient = RestClient.create();

  private String cachedToken;
  private Instant tokenExpiresAt;

  /**
   * 공통 GET 요청
   */
  public String get(String path) {
    return restClient.get()
        .uri(properties.getApiUrl() + path)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getValidAccessToken())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(String.class);
  }

  // SmartstoreRestClient.java 내부
  public String post(String path, String jsonBody) {
    // (토큰 발급 등 기존 인증 로직이 있다면 통과한 후)
    try {
      return restClient.post()
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + getValidAccessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .body(jsonBody)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      throw new RuntimeException("스마트스토어 POST 호출 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 공통 PUT 요청
   */
  public String put(String path, Object body) {
    return restClient.put()
        .uri(properties.getApiUrl() + path)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getValidAccessToken())
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(String.class);
  }

  /**
   * 공통 DELETE 요청
   */
  public void delete(String path) {
    try {
      restClient.delete()
          .uri(properties.getApiUrl() + path)
          // 스마트스토어는 getValidAccessToken() 호출
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + getValidAccessToken())
          .retrieve()
          .toBodilessEntity(); // 응답 본문이 필요 없으므로 깔끔하게 버림
    } catch (Exception e) {
      log.error("[DELETE Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("API DELETE 호출 실패", e);
    }
  }

  // =========================================================================
  // [내부 헬퍼] 토큰 관리 및 BCrypt 서명 로직 (기존 SmartstoreApiUtil 통합)
  // =========================================================================
  private synchronized String getValidAccessToken() {
    // 토큰이 없거나 만료 1분 전이면 새로 발급
    if (cachedToken == null || tokenExpiresAt == null || tokenExpiresAt.minusSeconds(60).isBefore(Instant.now())) {
      issueNewToken();
    }
    return cachedToken;
  }

  private void issueNewToken() {
    long timestamp = System.currentTimeMillis();
    String password = properties.getClientId() + "_" + timestamp;
    String hashedPw = BCrypt.hashpw(password, properties.getClientSecret());
    String clientSecretSign = Base64.getEncoder().encodeToString(hashedPw.getBytes(StandardCharsets.UTF_8));

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "client_credentials");
    formData.add("client_id", properties.getClientId());
    formData.add("timestamp", String.valueOf(timestamp));
    formData.add("client_secret_sign", clientSecretSign);
    formData.add("type", "SELF");

    try {
      JsonNode response = restClient.post()
          .uri("https://api.commerce.naver.com/external/v1/oauth2/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(JsonNode.class);

      if (response != null && response.has("access_token")) {
        this.cachedToken = response.get("access_token").asText();
        long expiresIn = response.get("expires_in").asLong(); // 보통 10800초 (3시간)
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        log.info("✅ 스마트스토어 토큰 발급 완료");
      }
    } catch (Exception e) {
      log.error("❌ 스마트스토어 토큰 발급 실패: {}", e.getMessage());
      throw new RuntimeException("스마트스토어 인증 실패", e);
    }
  }
}
