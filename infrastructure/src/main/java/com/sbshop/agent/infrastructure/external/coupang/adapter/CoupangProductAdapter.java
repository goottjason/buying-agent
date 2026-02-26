package com.sbshop.agent.infrastructure.external.coupang.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
// (기존에 쓰시던 CoupangApiUtil 또는 CoupangWebClient를 주입받는다고 가정합니다)
import com.sbshop.agent.infrastructure.external.coupang.client.CoupangWebClient;
import com.sbshop.agent.infrastructure.external.coupang.config.CoupangProperties;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangProductAdapter implements
    MarketProductReaderPort,
    MarketDataExtractorPort,
    MarketCommandPort {

  private final CoupangProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient = RestClient.create();

  // =========================================================================
  // 쿠팡 상품 매핑 캐시 (SKU -> 쿠팡 sellerProductId)
  // =========================================================================
  private Map<String, String> coupangSkuCache = null;
  private final File CACHE_FILE = new File("coupang_sku_cache.json");

  // [핵심 1] 팩토리가 나를 '쿠팡' 담당으로 인식하게 합니다!
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  // =========================================================================
  // 1. Reader Port: 쿠팡 상품 검색 (정찰용 우회)
  // =========================================================================
  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    log.warn("⏩ [쿠팡] 쿠팡은 역방향 동기화(Reverse Sync)를 사용하므로 sku로 조회를 지원하지 않습니다.");
    return Optional.empty();
  }

  // =========================================================================
  // 2. Extractor Port: 상세 데이터 추출 및 정찰
  // =========================================================================
  @Override
  public MarketExtractedData extractInitialProductData(String marketProductNo) {
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductNo;

    try {
      String responseJson = executeCoupangRequest("GET", path, null);
      com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseJson);
      com.fasterxml.jackson.databind.JsonNode dataNode = rootNode.path("data");

      // =========================================================================
      // 🚀 1. 식별자(Identifiers) 바구니 싹쓸이 (이게 핵심입니다!)
      // =========================================================================
      Map<String, String> identifiers = new java.util.HashMap<>();
      identifiers.put("sellerProductId", dataNode.path("sellerProductId").asText(""));
      identifiers.put("productId", dataNode.path("productId").asText(""));
      identifiers.put("vendorId", dataNode.path("vendorId").asText(""));

      // 🚀 쿠팡의 진짜 옵션 식별자들은 items 배열 안에 숨어있습니다!
      com.fasterxml.jackson.databind.JsonNode firstItem = dataNode.path("items").get(0);
      if (firstItem != null && !firstItem.isMissingNode()) {
        identifiers.put("vendorItemId", firstItem.path("vendorItemId").asText(""));
        identifiers.put("itemId", firstItem.path("itemId").asText(""));
        identifiers.put("sellerProductItemId", firstItem.path("sellerProductItemId").asText(""));
        // ⭐️ 우리 Product를 찾을 마스터 열쇠 (이게 없으면 역방향 동기화 불가!)
        identifiers.put("externalVendorSku", firstItem.path("externalVendorSku").asText(""));
      }

      // =========================================================================
      // 🚀 2. 고부가가치 마스터 데이터 추출
      // =========================================================================
      String brand = dataNode.path("brand").asText("");
      String manufacturer = dataNode.path("manufacture").asText("");
      String categoryCode = dataNode.path("displayCategoryCode").asText("");
      String generalName = dataNode.path("generalProductName").asText("");
      String barcode = "";

      if (firstItem != null && !firstItem.isMissingNode()) {
        barcode = firstItem.path("barcode").asText("");
      }

      Map<String, Object> rawDataMap = objectMapper.convertValue(rootNode, new com.fasterxml.jackson.core.type.TypeReference<>() {});

      return MarketExtractedData.builder()
          .isMasterData(true) // 🚀 마스터 DB 업데이트 승인!
          .marketIdentifiers(identifiers) // 🚀 꽉 채운 7종 세트 전달!

          // 기존 데이터 덮어쓰기 방지
          .name(null)
          .originalName(null)
          .salePrice(null)
          .stock(null)
          .detailHtml(null)
          .images(null)

          // 🚀 채워 넣을 알짜 데이터
          .brand(brand)
          .manufacturer(manufacturer)
          .categoryCode(categoryCode)
          .barcode(barcode)
          .generalProductName(generalName)

          .rawData(rawDataMap)
          .build();

    } catch (Exception e) {
      log.error("❌ 쿠팡 상품 탐색 실패 ({}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("쿠팡 파싱 중 오류 발생", e);
    }
  }

  // =========================================================================
  // 3. Command Port: 상태 변경 (임시 껍데기)
  // =========================================================================
  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    log.info("📝 [쿠팡 Command] 메모 업데이트 껍데기 호출됨 - sellerProductId: {}", marketProductNo);
  }

  // =========================================================================
  // 🔒 쿠팡 전용 HMAC 암호화 서명 생성기 (레거시 코드 완벽 이식)
  // =========================================================================
  private String generateAuthHeader(String method, String path, String queryString) throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd'T'HHmmss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 반드시 GMT 기준
    String datetime = sdf.format(new Date());

    String message = datetime + method + path + queryString;

    Mac hmac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(properties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    hmac.init(secretKey);

    byte[] rawHmac = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder();
    for (byte b : rawHmac) {
      hexString.append(String.format("%02x", b));
    }

    return String.format(
        "CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
        properties.getAccessKey(), datetime, hexString.toString()
    );
  }

  // =========================================================================
  // 🔒 쿠팡 전용 안전 통신 로직 (Spring 자동 인코딩 방지 + GZIP 디코딩 완벽 지원)
  // =========================================================================
  private String executeCoupangRequest(String method, String path, Map<String, String> params) throws Exception {
    // 1. GMT 표준 시간 생성
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyMMdd'T'HHmmss'Z'");
    sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    String datetime = sdf.format(new java.util.Date());

    // 2. 파라미터 조립 (순수 문자열 유지!)
    String queryString = "";
    if (params != null && !params.isEmpty()) {
      queryString = params.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(java.util.stream.Collectors.joining("&"));
    }

    // 3. HMAC 서명 생성
    String message = datetime + method + path + queryString;
    javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
    javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(properties.getSecretKey().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    hmac.init(secretKey);
    byte[] rawHmac = hmac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder();
    for (byte b : rawHmac) {
      hexString.append(String.format("%02x", b));
    }

    String authorization = String.format(
        "CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
        properties.getAccessKey(), datetime, hexString.toString()
    );

    String fullUrl = properties.getApiUrl() + path + (queryString.isEmpty() ? "" : "?" + queryString);

    // 4. HttpURLConnection 통신 (Spring RestClient 개입 원천 차단)
    java.net.URL url = new java.net.URL(fullUrl);
    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setRequestMethod(method);
    conn.setRequestProperty("Authorization", authorization);
    conn.setRequestProperty("Content-Type", "application/json");

    int responseCode = conn.getResponseCode();
    java.io.InputStream inputStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();

    // 🚀 핵심: 쿠팡의 외계어(GZIP) 압축 해제 로직!
    if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
      inputStream = new java.util.zip.GZIPInputStream(inputStream);
    }

    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      response.append(line);
    }
    br.close();

    // 만약 400, 403 등 에러가 나면 읽어낸 에러 JSON을 던져버림
    if (responseCode >= 400) {
      throw new RuntimeException("HTTP " + responseCode + " - " + response.toString());
    }

    return response.toString();
  }

  // =========================================================================
  // 🚀 [신규 기능] 쿠팡 전체 상품 ID(sellerProductId) 싹쓸이 기능
  // =========================================================================
  public List<String> fetchAllSellerProductIds() {
    log.info("🚀 [쿠팡 역방향 수집] 쿠팡에 등록된 전체 상품 ID 목록을 불러옵니다...");
    List<String> allIds = new ArrayList<>();
    String nextToken = "";
    boolean hasNext = true;

    while (hasNext) {
      try {
        String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";
        Map<String, String> params = new HashMap<>();
        params.put("vendorId", properties.getVendorId());
        params.put("maxPerPage", "50");
        if (!nextToken.isEmpty()) {
          params.put("nextToken", nextToken);
        }

        String responseJson = executeCoupangRequest("GET", path, params);
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode dataNode = rootNode.path("data");

        if (dataNode == null || dataNode.isMissingNode() || !dataNode.isArray() || dataNode.isEmpty()) {
          break;
        }

        for (JsonNode productNode : dataNode) {
          allIds.add(productNode.path("sellerProductId").asText());
        }

        JsonNode nextTokenNode = rootNode.path("nextToken");
        if (nextTokenNode != null && !nextTokenNode.isMissingNode() && !nextTokenNode.asText().isEmpty()) {
          nextToken = nextTokenNode.asText();
        } else {
          hasNext = false;
        }
        Thread.sleep(500); // 0.5초 대기 (Rate Limit 방어)

      } catch (Exception e) {
        log.error("❌ 쿠팡 목록 페이징 에러: {}", e.getMessage());
        break;
      }
    }
    log.info("📦 [쿠팡 역방향 수집] 총 {}개의 상품 ID를 수집했습니다!", allIds.size());
    return allIds;
  }

  // =========================================================================
  // 🚀 [신규] 쿠팡의 자체상품코드(SKU)를 진짜로 덮어쓰는 Command
  // =========================================================================
  public boolean updateExternalVendorSku(String vendorItemId, String realSku) {
    // 쿠팡 옵션(VendorItem) 부분 수정 API (주의: API 버전에 따라 전체 페이로드가 필요할 수도 있습니다)
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/vendor-items/" + vendorItemId;

    try {
      // 🚀 쿠팡의 경우 옵션 정보 전체를 덮어써야 할 수도 있지만,
      // SKU만 살짝 덮어쓰는 꼼수(또는 sales-parameters 엔드포인트)를 활용합니다.
      // (만약 이 단순 PUT으로 쿠팡이 에러를 뱉는다면, 기존 JSON을 GET해서 externalVendorSku만 싹 바꿔서 다시 PUT해야 합니다!)
      Map<String, Object> body = new java.util.HashMap<>();
      body.put("vendorItemId", Long.parseLong(vendorItemId));
      body.put("externalVendorSku", realSku); // 🚀 진짜 SKU 주입!

      String bodyJson = objectMapper.writeValueAsString(body);

      // 아까 만든 안전 통신 메서드를 PUT 용으로 살짝 수정하거나 재활용합니다.
      String responseJson = executeCoupangRequestWithBody("PUT", path, null, bodyJson);

      return responseJson.contains("SUCCESS") || responseJson.contains("200");

    } catch (Exception e) {
      log.error("❌ 쿠팡 SKU 교정 통신 실패 (vendorItemId: {}): {}", vendorItemId, e.getMessage());
      return false;
    }
  }

  // =========================================================================
  // 🔒 쿠팡 전용 안전 통신 로직 (Request Body 포함 버전 - PUT, POST 용)
  // =========================================================================
  private String executeCoupangRequestWithBody(String method, String path, java.util.Map<String, String> params, String bodyJson) throws Exception {
    // 1. GMT 표준 시간 생성 (쿠팡 서명용)
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyMMdd'T'HHmmss'Z'");
    sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    String datetime = sdf.format(new java.util.Date());

    // 2. 파라미터 조립 (순수 문자열 유지하여 서명 불일치 방지)
    String queryString = "";
    if (params != null && !params.isEmpty()) {
      queryString = params.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(java.util.stream.Collectors.joining("&"));
    }

    // 3. HMAC 서명 생성 알고리즘
    String message = datetime + method + path + queryString;
    javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
    javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
        properties.getSecretKey().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"
    );
    hmac.init(secretKey);
    byte[] rawHmac = hmac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder();
    for (byte b : rawHmac) {
      hexString.append(String.format("%02x", b));
    }

    String authorization = String.format(
        "CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s",
        properties.getAccessKey(), datetime, hexString.toString()
    );

    String fullUrl = properties.getApiUrl() + path + (queryString.isEmpty() ? "" : "?" + queryString);

    // 4. HttpURLConnection 통신 설정
    java.net.URL url = new java.net.URL(fullUrl);
    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

    // 🚀 무한 대기(Hang) 방지를 위한 타임아웃 10초 설정
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(10000);

    conn.setRequestMethod(method);
    conn.setRequestProperty("Authorization", authorization);
    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

    // 5. 🚀 Request Body 데이터 쓰기 (PUT/POST)
    if (bodyJson != null && !bodyJson.isEmpty()) {
      conn.setDoOutput(true); // Body를 보내겠다는 설정
      try (java.io.OutputStream os = conn.getOutputStream()) {
        byte[] input = bodyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
        os.flush();
      }
    }

    // 6. 응답 코드 확인 및 스트림 가져오기
    int responseCode = conn.getResponseCode();
    java.io.InputStream inputStream;
    if (responseCode >= 200 && responseCode < 300) {
      inputStream = conn.getInputStream();
    } else {
      inputStream = conn.getErrorStream();
      if (inputStream == null) {
        throw new RuntimeException("HTTP " + responseCode + " - 응답 스트림이 없습니다.");
      }
    }

    // 7. 🚀 GZIP 압축 해제 처리 (쿠팡 외계어 에러 방어)
    if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
      inputStream = new java.util.zip.GZIPInputStream(inputStream);
    }

    // 8. 응답 본문 깔끔하게 읽어오기
    StringBuilder response = new StringBuilder();
    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line);
      }
    }

    // 9. 실패 응답(4xx, 5xx)일 경우 예외를 던져서 상위에서 캐치하도록 함
    if (responseCode >= 400) {
      throw new RuntimeException("HTTP " + responseCode + " - " + response.toString());
    }

    return response.toString();
  }
}
