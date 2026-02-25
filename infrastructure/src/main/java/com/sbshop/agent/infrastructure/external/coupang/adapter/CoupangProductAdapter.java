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
  // 🚀 쿠팡 상품 매핑 캐시 (우리 SKU -> 쿠팡 sellerProductId)
  // =========================================================================
  private Map<String, String> coupangSkuCache = null;

  // 🚀 [핵심 1] 팩토리가 나를 '쿠팡' 담당으로 인식하게 합니다!
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  // =========================================================================
  // 1. Reader Port: 쿠팡 상품 검색 (정찰용 우회)
  // =========================================================================
  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    // 1. 캐시가 비어있다면 최초 1회 전체 로드 (무식하지만 가장 확실하고 빠른 방법)
    if (coupangSkuCache == null) {
      buildCoupangSkuCache();
    }

    // 2. 이후부터는 통신 없이 캐시에서 빛의 속도로 꺼내서 리턴!
    return Optional.ofNullable(coupangSkuCache.get(sku));
  }

  // =========================================================================
  // 2. Extractor Port: 상세 데이터 추출 및 정찰
  // =========================================================================
  @Override
  public MarketExtractedData extractInitialProductData(String marketProductNo) {
    // 쿠팡 상품 상세 조회 API 경로
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductNo;
    String url = properties.getApiUrl() + path;

    try {
      // 🚀 동적 암호화 헤더 생성!
      String authHeader = generateAuthHeader("GET", path, "");

      String responseJson = restClient.get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, authHeader) // 쿠팡 전용 CEA 서명 헤더
          .header(HttpHeaders.CONTENT_TYPE, "application/json")
          .retrieve()
          .body(String.class);

      JsonNode rootNode = objectMapper.readTree(responseJson);
      String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

      log.info("==================================================");
      log.info("📦 [쿠팡 탐색] sellerProductId: {}", marketProductNo);
      log.info("==================================================");
      log.info("\n{}", prettyJson); // 🚀 쿠팡의 엄청난 JSON 응답 확인!
      log.info("==================================================");

      Map<String, Object> rawDataMap = objectMapper.convertValue(rootNode, new TypeReference<>() {});

      return MarketExtractedData.builder()
          .isMasterData(false) // 쿠팡도 보조 마켓이므로 덮어쓰기 방지
          .marketIdentifiers(Map.of("sellerProductId", marketProductNo))
          .name("쿠팡 정찰중")
          .originalName("")
          .salePrice(BigDecimal.ZERO)
          .stock(0)
          .detailHtml("탐색 완료")
          .images(new ArrayList<>())
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
  // 🛠️ 쿠팡 전체 상품 목록 & 상세 조회 노가다 (최초 1회 실행)
  // =========================================================================
  private synchronized void buildCoupangSkuCache() {
    if (coupangSkuCache != null) return; // 멀티스레드 방어

    log.info("🚀 [쿠팡] 캐시가 없습니다. 쿠팡 전체 상품 목록을 조회하여 SKU 맵핑을 시작합니다! (최초 1회 실행, 3000개 기준 약 30~60분 소요)");
    Map<String, String> tempCache = new HashMap<>();

    String nextToken = "";
    boolean hasNext = true;
    List<String> allSellerProductIds = new ArrayList<>();

    // ---------------------------------------------------------------------
    // 1단계: 쿠팡에 등록된 모든 상품의 ID(sellerProductId) 싹쓸이
    // ---------------------------------------------------------------------
    while (hasNext) {
      try {
        String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";
        String queryString = "vendorId=" + properties.getVendorId() + "&maxPerPage=50";
        if (!nextToken.isEmpty()) {
          queryString += "&nextToken=" + nextToken;
        }

        // 🚀 핵심: 암호화 서명 시 path와 queryString을 분리해서 던져야 합니다!
        String authHeader = generateAuthHeader("GET", path, queryString);
        String fullUrl = properties.getApiUrl() + path + "?" + queryString;

        String responseJson = restClient.get()
            .uri(fullUrl)
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .retrieve()
            .body(String.class);

        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode dataNode = rootNode.path("data");

        if (dataNode == null || dataNode.isMissingNode() || !dataNode.isArray() || dataNode.size() == 0) {
          break;
        }

        for (JsonNode productNode : dataNode) {
          allSellerProductIds.add(productNode.path("sellerProductId").asText());
        }

        JsonNode nextTokenNode = rootNode.path("nextToken");
        if (nextTokenNode != null && !nextTokenNode.isMissingNode() && !nextTokenNode.asText().isEmpty()) {
          nextToken = nextTokenNode.asText();
        } else {
          hasNext = false;
        }

        // API Rate Limit 방어를 위해 1초 휴식
        Thread.sleep(1000);

      } catch (Exception e) {
        log.error("❌ 쿠팡 목록 페이징 중 에러 발생: {}", e.getMessage());
        break;
      }
    }

    log.info("📦 [쿠팡] 총 {}개의 상품 ID를 수집했습니다. 상세 조회를 통해 SKU(자체상품코드)를 추출합니다...", allSellerProductIds.size());

    // ---------------------------------------------------------------------
    // 2단계: 수집된 ID들로 단건 상세조회를 돌려서 SKU 찾아내기
    // ---------------------------------------------------------------------
    int count = 0;
    for (String sellerProductId : allSellerProductIds) {
      try {
        // 방금 만든 Extractor를 재활용하여 상세 정보를 뜯어옵니다.
        MarketExtractedData extractedData = extractInitialProductData(sellerProductId);

        // 식별자 바구니에서 우리의 SKU를 꺼냅니다.
        String sku = extractedData.marketIdentifiers().get("externalVendorSku");

        if (sku != null && !sku.isEmpty()) {
          tempCache.put(sku, sellerProductId);
        }

        count++;
        if (count % 10 == 0) {
          log.info("⏳ [쿠팡 SKU 맵핑 진행중] {} / {} 완료 (현재 맵핑된 SKU: {}개)", count, allSellerProductIds.size(), tempCache.size());
        }

        // 쿠팡 API Rate Limit 방어를 위해 상세조회 후 1초 휴식
        Thread.sleep(1000);

      } catch (Exception e) {
        log.error("❌ 쿠팡 단건 조회 매핑 에러 ({}): {}", sellerProductId, e.getMessage());
      }
    }

    this.coupangSkuCache = tempCache;
    log.info("🎉 [쿠팡 SKU 맵핑 완료] 총 {}개의 상품이 메모리 캐시에 완벽하게 저장되었습니다!", tempCache.size());
  }
}
