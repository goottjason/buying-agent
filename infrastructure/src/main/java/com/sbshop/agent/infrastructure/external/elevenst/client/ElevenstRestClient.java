package com.sbshop.agent.infrastructure.external.elevenst.client;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.elevenst.config.ElevenstProperties;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
}
