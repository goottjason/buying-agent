package com.sbshop.agent.infrastructure.client.elevenst.client;

import com.sbshop.agent.infrastructure.client.elevenst.config.ElevenstProperties;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstRestClient {
  private final ElevenstProperties properties;
  private final RestClient restClient = RestClient.create();
  private static final Charset EUC_KR = Charset.forName("EUC-KR");

  /**
   * 11번가 공통 GET 요청 (응답이 XML로 옵니다)
   */
  public String get(String path) {
    try {
      byte[] responseBytes = restClient.get()
          .uri(properties.getApiUrl() + path)
          .header("openapikey", properties.getApiKey())
          .acceptCharset(EUC_KR)
          .retrieve()
          .body(byte[].class); // EUC-KR 한글 깨짐 방지를 위해 byte로 받아서 직접 변환!

      return responseBytes != null ? new String(responseBytes, EUC_KR) : "";
    } catch (Exception e) {
      log.error("[Elevenst GET Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("11번가 API 호출 실패", e);
    }
  }

  // ElevenstRestClient.java 내부
  public String postXml(String path, String xmlBody) {
    try {
      return restClient.post()
          .uri(properties.getApiUrl() + path)
          .header("openapikey", properties.getApiKey()) // 11번가 인증 헤더 (명세서 확인 필요)
          .contentType(org.springframework.http.MediaType.TEXT_XML) // 🚀 핵심: XML 타입!
          .body(xmlBody)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      throw new RuntimeException("11번가 POST(XML) 호출 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 11번가 공통 POST/PUT 요청
   */
  public String requestWithBody(String method, String path, String xmlBody) {
    try {
      byte[] responseBytes = restClient.method(org.springframework.http.HttpMethod.valueOf(method))
          .uri(properties.getApiUrl() + path)
          .header("openapikey", properties.getApiKey())
          .contentType(new MediaType("text", "xml", EUC_KR))
          .body(xmlBody.getBytes(EUC_KR))
          .retrieve()
          .body(byte[].class);

      return responseBytes != null ? new String(responseBytes, EUC_KR) : "";
    } catch (Exception e) {
      log.error("[Elevenst {} Error] path: {}, msg: {}", method, path, e.getMessage());
      throw new RuntimeException("11번가 API " + method + " 호출 실패", e);
    }
  }

  /**
   * 🚀 [신규] 11번가 공통 PUT 요청
   */
  public String put(String path, Object body) {
    // 💡 참고: 11번가 오픈API는 대부분 XML 규격을 사용합니다.
    // 만약 실무 연동 시 11번가 서버가 JSON을 거부한다면, 이 부분에서 body(Map)를
    // XML 문자열로 변환한 뒤 기존의 requestWithBody("PUT", path, xmlString)를 호출하도록 수정해야 합니다.
    try {
      return restClient.put()
          .uri(properties.getApiUrl() + path)
          .header("openapikey", properties.getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.error("[Elevenst PUT Error] path: {}, msg: {}", path, e.getMessage());
      throw new RuntimeException("11번가 API PUT 호출 실패", e);
    }
  }
}
