package com.sbshop.agent.infrastructure.client.cafe24.client;

import com.sbshop.agent.infrastructure.client.cafe24.auth.Cafe24TokenManager;
import com.sbshop.agent.infrastructure.client.cafe24.config.Cafe24Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24RestClient {

  private final Cafe24Properties properties;
  private final Cafe24TokenManager tokenManager;
  private final RestClient restClient = RestClient.create();

  public String get(String path) {
    try {
      return restClient.get()
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class); // 일단 String(JSON)으로 받습니다.
    } catch (Exception e) {
      log.error("[Cafe24 GET Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("Cafe24 API 호출 실패", e);
    }
  }
  /**
   * 공통 DELETE 요청
   */
  public void delete(String path) {
    try {
      restClient.delete()
          .uri(properties.getApiUrl() + path)
          // 스마트스토어는 getValidAccessToken() 호출
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .retrieve()
          .toBodilessEntity(); // 응답 본문이 필요 없으므로 깔끔하게 버림
    } catch (Exception e) {
      log.error("[DELETE Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("API DELETE 호출 실패", e);
    }
  }

  // 💡 나중에 [2] 유령 상품 삭제를 위한 API도 이렇게 뚝딱 만들 수 있습니다!
    /*
    public void executeDelete(String path) {
        String cleanPath = path.startsWith("/api/v2") ? path.replaceFirst("/api/v2", "") : path;
        restClient.delete()
                .uri(properties.getApiUrl() + cleanPath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
                .header("X-Cafe24-Api-Version", API_VERSION)
                .retrieve()
                .toBodilessEntity();
    }
    */
}