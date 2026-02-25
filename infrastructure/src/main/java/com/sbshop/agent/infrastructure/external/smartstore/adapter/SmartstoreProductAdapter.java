package com.sbshop.agent.infrastructure.external.smartstore.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.smartstore.auth.SmartstoreTokenManager;
import com.sbshop.agent.infrastructure.external.smartstore.client.SmartstoreWebClient;
import com.sbshop.agent.infrastructure.external.smartstore.config.SmartstoreProperties;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreProductAdapter implements
    MarketProductReaderPort,
    MarketDataExtractorPort,
    MarketCommandPort {

  private final SmartstoreProperties properties;
  private final SmartstoreTokenManager tokenManager;
  private final ObjectMapper objectMapper;
  private final RestClient restClient = RestClient.create();

  // 🚀 [핵심 1] 팩토리에게 "나는 스마트스토어 담당이야!" 라고 알려줍니다.
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.SMARTSTORE; // Enum에 SMARTSTORE가 없으면 추가해주세요!
  }

  // =========================================================================
  // 1. Reader Port: SKU로 마켓 상품 번호 찾기 (껍데기)
  // =========================================================================
  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    // 스마트스토어 상품 검색 API
    String url = properties.getApiUrl() + "/v1/products/search";

    // 검색 조건: 판매자 관리 코드(SKU)로 검색하겠다고 명시
    Map<String, Object> requestBody = new java.util.HashMap<>();
    requestBody.put("searchKeywordType", "SELLER_CODE");
    requestBody.put("sellerManagementCode", sku);
    requestBody.put("page", 1);
    requestBody.put("size", 10); // 보통 SKU당 1개만 나오므로 10개면 충분합니다.

    try {
      String responseJson = restClient.post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .contentType(MediaType.APPLICATION_JSON) // POST & JSON 바디 필수
          .body(requestBody)
          .retrieve()
          .body(String.class);

      JsonNode rootNode = objectMapper.readTree(responseJson);
      JsonNode contents = rootNode.path("contents"); // 스마트스토어는 결과 배열을 'contents'에 담아줍니다.

      // 배열에 결과가 1개라도 있다면 첫 번째 상품의 'originProductNo'를 추출합니다.
      if (contents.isArray() && !contents.isEmpty()) {
        // 스마트스토어의 마스터 키워드는 'originProductNo' (원상품번호) 입니다.
        String originProductNo = contents.get(0).path("originProductNo").asText();
        log.info("🎯 [스마트스토어 검색 성공] SKU: {} -> originProductNo: {}", sku, originProductNo);
        return Optional.of(originProductNo);
      } else {
        log.warn("🔍 [검색 실패] 스마트스토어 SKU: {} -> 응답에 상품이 없습니다.", sku);
      }

    } catch (Exception e) {
      log.error("❌ 스마트스토어 SKU({}) 검색 파싱 실패: {}", sku, e.getMessage());
    }

    return Optional.empty();
  }

  // =========================================================================
  // 2. Extractor Port: 데이터 추출 및 가공 (🚀 여기에 정찰기를 달았습니다!)
  // =========================================================================
  @Override
  public MarketExtractedData extractInitialProductData(String marketProductNo) {
    String url = properties.getApiUrl() + "/v2/products/origin-products/" + marketProductNo;

    try {
      // API 호출
      String responseJson = restClient.get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .retrieve()
          .body(String.class);

      // JsonNode로 읽어서 예쁘게(Pretty Print) 콘솔에 출력!
      JsonNode rootNode = objectMapper.readTree(responseJson);
      String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

      /*log.info("==================================================");
      log.info("📦 [스마트스토어 탐색] 원본 상품번호: {}", marketProductNo);
      log.info("==================================================");
      log.info("\n{}", prettyJson);
      log.info("==================================================");*/

      // 지휘관(Processor)이 에러를 뱉지 않도록 임시 깡통 객체를 리턴해줍니다.
      Map<String, Object> rawDataMap = objectMapper.convertValue(rootNode, new TypeReference<>() {});

      // =========================================================================
      // 🚀 식별자(Identifiers) 바구니 조립: originProductNo + channelProductNo
      // =========================================================================
      Map<String, String> marketIdentifiers = new HashMap<>();

      // 1. 기본 식별자: 원상품번호
      marketIdentifiers.put("originProductNo", marketProductNo);

      // 2. 추가 식별자: 채널상품번호 (안전하게 추출)
      JsonNode channelNode = rootNode.path("smartstoreChannelProduct");
      if (!channelNode.isMissingNode() && channelNode.hasNonNull("channelProductNo")) {
        String channelProductNo = channelNode.get("channelProductNo").asText();
        marketIdentifiers.put("channelProductNo", channelProductNo);
      }

      // 3. 🚀 미래를 위한 교차 검증 키: 판매자 관리 코드 (SKU)
      JsonNode sellerCodeInfoNode = rootNode.path("originProduct")
          .path("detailAttribute")
          .path("sellerCodeInfo");
      if (!sellerCodeInfoNode.isMissingNode() && sellerCodeInfoNode.hasNonNull("sellerManagementCode")) {
        marketIdentifiers.put("sellerManagementCode", sellerCodeInfoNode.get("sellerManagementCode").asText());
      }

      // =========================================================================
      return MarketExtractedData.builder()
          .isMasterData(false) // Product 업데이트 스킵 지시!
          .marketIdentifiers(marketIdentifiers)
          .name("스마트스토어 정찰중") // 임시 값
          .originalName("")
          .salePrice(BigDecimal.ZERO)
          .stock(0)
          .detailHtml("탐색 완료")
          .images(new ArrayList<>())
          .rawData(rawDataMap) // 원본 바구니는 살려둡니다
          .build();

    } catch (Exception e) {
      log.error("❌ 스마트스토어 상품 탐색 실패 ({}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("스마트스토어 파싱 중 오류 발생", e);
    }
  }

  // =========================================================================
  // 3. Command Port: 상태 변경 및 메모 (껍데기)
  // =========================================================================
  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {

    String url = properties.getApiUrl() + "/v2/products/origin-products/" + marketProductNo;

    try {
      // 1. 기존 상품 정보 가져오기 (GET)
      String responseJson = restClient.get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .retrieve()
          .body(String.class);

      Map<String, Object> responseMap = objectMapper.readValue(responseJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

      @SuppressWarnings("unchecked")
      Map<String, Object> originProduct = (Map<String, Object>) responseMap.get("originProduct");

      // 2. A/S 정보(afterServiceInfo) 객체 추출
      @SuppressWarnings("unchecked")
      Map<String, Object> detailAttribute = (Map<String, Object>) originProduct.getOrDefault("detailAttribute", new HashMap<>());

      @SuppressWarnings("unchecked")
      Map<String, Object> afterServiceInfo = (Map<String, Object>) detailAttribute.getOrDefault("afterServiceInfo", new HashMap<>());

      // 🚀 핵심: 바뀐 A/S 전화번호 적용 및 안내 문구 살짝 비틀기(무조건 수정일시 갱신 유도)
      afterServiceInfo.put("afterServiceTelephoneNumber", "010-2597-2480"); // 바뀐 번호
      afterServiceInfo.put("afterServiceGuideContent", "상품 상세설명 참조"); // 띄어쓰기 교정으로 확실한 PUT 유도

      // 조립된 A/S 객체를 다시 꽂아넣기
      detailAttribute.put("afterServiceInfo", afterServiceInfo);
      originProduct.put("detailAttribute", detailAttribute);

      // =========================================================================
      // 🚀 [추가] 스마트스토어 API의 악명 높은 품절(OUTOFSTOCK) 상태 튕겨내기 방어!
      // (GET으로 받은 OUTOFSTOCK을 PUT이 거부하므로 SALE + 재고 0으로 변환)
      // =========================================================================
      if ("OUTOFSTOCK".equals(originProduct.get("statusType"))) {
        originProduct.put("statusType", "SALE");
        originProduct.put("stockQuantity", 0);
      }
      // =========================================================================

      // 3. 수정된 정보로 스마트스토어 덮어쓰기 (PUT) -> 스마트스토어의 수정일시 갱신!
      Map<String, Object> putBody = new HashMap<>();
      putBody.put("originProduct", originProduct);

      restClient.put()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.getValidAccessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .body(putBody)
          .retrieve()
          .toBodilessEntity();

      log.info("📝 스마트스토어 상품({}) A/S 정보 갱신(수정일시 업데이트) 완료!", marketProductNo);

    } catch (Exception e) {
      log.error("❌ 스마트스토어 A/S 정보 갱신 실패 ({}): {}", marketProductNo, e.getMessage());
    }
  }


  /*@Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    // 스마트스토어는 판매자관리코드(sellerManagementCode)로 상품을 조회할 수 있습니다.
    String path = "/v1/products/search?sellerManagementCode=" + sku;

    try {
      String responseJson = webClient.get(path);
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode contents = root.path("contents");

      if (contents.isArray() && contents.size() > 0) {
        // 스마트스토어의 고유 상품 번호 반환
        return Optional.of(contents.get(0).path("originProductNo").asText());
      }
    } catch (Exception e) {
      log.error("스마트스토어 SKU({}) 검색 실패: {}", sku, e.getMessage());
    }
    return Optional.empty();
  }

  @Override
  public MarketExtractedData getProductDetailsByMarketProductNo(String marketProductNo) {
    String path = "/v2/products/origin-products/" + marketProductNo;

    try {
      String responseJson = webClient.get(path);
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode originProduct = root.path("originProduct");

      // 1. 상세 HTML 추출
      String detailHtml = originProduct.path("detailContent").asText("");

      // 2. 이미지 추출 (대표 이미지 + 추가 이미지)
      List<String> images = new ArrayList<>();
      JsonNode imagesNode = originProduct.path("images");

      if (imagesNode.has("representativeImage")) {
        images.add(imagesNode.path("representativeImage").path("url").asText());
      }
      if (imagesNode.has("optionalImages") && imagesNode.path("optionalImages").isArray()) {
        for (JsonNode optImg : imagesNode.path("optionalImages")) {
          images.add(optImg.path("url").asText());
        }
      }

      return MarketExtractedData.builder()
          .detailHtml(detailHtml)
          .images(images)
          .build();

    } catch (Exception e) {
      log.error("스마트스토어 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("스마트스토어 상품 파싱 중 오류 발생");
    }
  }

  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 🚀 스마트스토어는 부분 수정이 안 되므로, GET으로 다 가져와서 고친 후 PUT으로 던집니다. (레거시 방식 유지)
    String path = "/v2/products/origin-products/" + marketProductNo;

    try {
      // 1. 기존 데이터 전체 조회
      String getResponseJson = webClient.get(path);
      Map<String, Object> getResponseMap = objectMapper.readValue(getResponseJson, new TypeReference<>() {});

      @SuppressWarnings("unchecked")
      Map<String, Object> originProduct = (Map<String, Object>) getResponseMap.get("originProduct");

      // 2. 메모 덮어쓰기 (판매자 관리 코드 또는 사용자 정의 필드 활용)
      originProduct.put("sellerManagementCode", syncMessage);

      // 3. 재포장 후 PUT 요청
      Map<String, Object> body = new HashMap<>();
      body.put("originProduct", originProduct);

      webClient.put(path, body);
      log.info("스마트스토어 상품({})에 동기화 메모 남기기 성공", marketProductNo);

    } catch (Exception e) {
      log.error("스마트스토어 메모 업데이트 실패 (ID: {}): {}", marketProductNo, e.getMessage());
    }
  }*/
}