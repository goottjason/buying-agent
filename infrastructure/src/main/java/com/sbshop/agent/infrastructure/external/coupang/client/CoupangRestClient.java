package com.sbshop.agent.infrastructure.external.coupang.client;

import com.sbshop.agent.infrastructure.external.coupang.config.CoupangProperties;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
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
  private String generateHmacSignature(String method, String path) {
    try {
      // 1. 시간 세팅 (UTC 기준)
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd'T'HHmmss'Z'");
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      String datetime = dateFormat.format(new Date());

      // 2. 서명에 쓰일 평문 만들기
      String message = datetime + method + path;

      // 3. SecretKey로 HmacSHA256 암호화
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(properties.getSecretKey().getBytes(
          StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

      // 4. Hex 스트링으로 변환
      StringBuilder signatureHex = new StringBuilder();
      for (byte b : signatureBytes) {
        signatureHex.append(String.format("%02x", b));
      }

      // 5. 최종 쿠팡 Authorization 헤더 포맷 조립
      return String.format("CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
          properties.getAccessKey(), datetime, signatureHex.toString());

    } catch (Exception e) {
      log.error("쿠팡 HMAC 서명 생성 실패", e);
      throw new RuntimeException("쿠팡 인증 키 생성 중 오류 발생");
    }
  }
}
