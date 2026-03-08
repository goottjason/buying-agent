package com.sbshop.agent.infrastructure.client.coupang.client;

import com.sbshop.agent.infrastructure.client.coupang.config.CoupangProperties;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangRestClient {
  private final CoupangProperties properties;
  private final RestClient restClient = RestClient.create();

  /**
   * 쿠팡 PUT API 호출 (판매 중지 등 상태 업데이트용)
   */
  public String put(String path, String requestBody) {
    String authorization = generateHmacSignature("PUT", path);

    try {
      return restClient.put()
          .uri(properties.getApiUrl() + path)
          .header(org.springframework.http.HttpHeaders.AUTHORIZATION, authorization)
          .header("X-Requested-By", properties.getVendorId()) // 🚀 잊지 말아야 할 헤더!
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .body(requestBody) // 빈 객체 "{}" 전송용
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("[Coupang PUT Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("쿠팡 API PUT 호출 실패", e);
    }
  }

  /**
   * 쿠팡 공통 GET 요청
   */
  public String get(String path) {
    // 1. 쿠팡은 요청할 때마다 찰나의 시간으로 새로운 서명(Authorization Header)을 만들어야 합니다.
    String authorization = generateHmacSignature("GET", path);

    try {
      // 2. 최신 RestClient로 깔끔하게 쏩니다!
      return restClient.get()
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, authorization)
          .header("X-Requested-By", properties.getVendorId())
          .header("X-EXTENDED-TIMEOUT", "90000") // 쿠팡 권장 타임아웃 헤더
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("[Coupang GET Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("쿠팡 API GET 호출 실패", e);
    }
  }

  /**
   * 쿠팡 공통 DELETE 요청
   */
  public void delete(String path) {
    // 🚀 개발자님의 전설의 무기: DELETE 메서드로 서명 생성!
    String authorization = generateHmacSignature("DELETE", path);

    try {
      restClient.delete()
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, authorization)
          .header("X-Requested-By", properties.getVendorId()) // 🚀 잊지 말아야 할 헤더!
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("[Coupang DELETE Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("쿠팡 API DELETE 호출 실패", e);
    }
  }

  /**
   * 쿠팡 공통 POST/PUT 요청 (동기화 때 메모 수정 등에 사용)
   */
  public String requestWithBody(String method, String path, Object body) {
    String authorization = generateHmacSignature(method, path);

    try {
      return restClient.method(org.springframework.http.HttpMethod.valueOf(method))
          .uri(properties.getApiUrl() + path)
          .header(HttpHeaders.AUTHORIZATION, authorization)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("[Coupang {} Error] path: {}, msg: {}", method, path, e.getMessage());
      throw new RuntimeException("쿠팡 API " + method + " 호출 실패", e);
    }
  }

  // =========================================================================
  // [내부 헬퍼] 기존 CoupangApiUtil 에 있던 HMAC 서명 생성 로직을 여기로 은닉!
  // =========================================================================
  private String generateHmacSignature(String method, String url) {
    // 🚀 1. URL에서 Path와 Query String을 분리합니다!
    String path = url;
    String query = "";

    if (url.contains("?")) {
      String[] parts = url.split("\\?", 2);
      path = parts[0];
      query = parts[1]; // 예: "maxPerPage=100"
    }

    // 2. 시간 생성 (yyMMdd'T'HHmmss'Z')
    String datetime = ZonedDateTime.now(ZoneId.of("UTC"))
        .format(DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss'Z'"));

    // 3. 쿠팡이 요구하는 암호화 메시지 원문 조립 (순서가 매우 중요합니다)
    String message = datetime + method + path + query;

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(
          properties.getSecretKey().getBytes(StandardCharsets.UTF_8),
          "HmacSHA256"
      );
      mac.init(secretKeySpec);

      byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

      // 🚀 외부 라이브러리(Hex) 대신 자바 17 내장 기능으로 깔끔하게 변환!
      String signature = java.util.HexFormat.of().formatHex(signatureBytes);

      return String.format("CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
          properties.getAccessKey(), datetime, signature);

    } catch (Exception e) {
      throw new RuntimeException("쿠팡 서명 생성 중 오류 발생", e);
    }
  }
}
