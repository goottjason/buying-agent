package com.sbshop.agent.infrastructure.external.cafe24.client;

import com.sbshop.agent.infrastructure.external.cafe24.auth.Cafe24TokenManager;
import com.sbshop.agent.infrastructure.external.cafe24.config.Cafe24Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class Cafe24WebClient {

  private final Cafe24Properties properties;
  private final Cafe24TokenManager tokenManager;
  private final RestClient restClient = RestClient.create();

  /**
   * 공통 GET 요청
   */
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
   * 공통 PUT 요청
   */
  public String put(String path, Object body) {
    try {
      // NOTE: Cafe24 API는 초당 호출 제한(Rate Limit)이 빡빡한 편이므로 안전하게 딜레이를 줍니다.
      Thread.sleep(350);

      return restClient.put()
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("[Cafe24 PUT Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("Cafe24 API 호출 실패", e);
    }
  }
}