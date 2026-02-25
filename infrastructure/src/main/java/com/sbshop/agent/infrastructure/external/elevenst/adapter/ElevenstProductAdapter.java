package com.sbshop.agent.infrastructure.external.elevenst.adapter;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketCommandPort;
import com.sbshop.agent.core.domain.product.port.MarketDataExtractorPort;
import com.sbshop.agent.core.domain.product.port.MarketProductReaderPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.elevenst.config.ElevenstProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ElevenstProductAdapter implements
    MarketProductReaderPort,
    MarketDataExtractorPort,
    MarketCommandPort {

  private final ElevenstProperties properties;
  private final RestClient restClient;

  public ElevenstProductAdapter(ElevenstProperties properties) {
    this.properties = properties;
    // 🚀 11번가 전용 RestClient: 매번 EUC-KR과 openapikey를 세팅하는 번거로움을 없앱니다.
    this.restClient = RestClient.builder()
        .baseUrl(properties.getApiUrl())
        .defaultHeader("openapikey", properties.getApiKey())
        .defaultHeader("Accept-Charset", "EUC-KR")
        .build();
  }

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.ELEVENST;
  }

  // =========================================================================
  // 1. Reader Port: SKU로 11번가 상품번호 찾기 (정찰용 껍데기)
  // =========================================================================
  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    /*// 11번가 상품 검색 API 엔드포인트 (상품 관리/상태 조회 서비스)
    // ※ 참고: 11번가 API 버전에 따라 /rest/prodstatusservice/stat/search 또는 /rest/prodservices/product/search 를 사용합니다.
    String url = properties.getApiUrl() + "/rest/prodstatusservice/stat/search";

    // 🚀 11번가 스타일: 자체상품코드(sellerPrdCd)로 검색하기 위한 XML 바디 생성
    String xmlBody = "<?xml version=\"1.0\" encoding=\"euc-kr\"?>\n" +
        "<ProductSearchRequest>\n" +
        "    <sellerPrdCd>" + sku + "</sellerPrdCd>\n" +
        "</ProductSearchRequest>";

    try {
      // EUC-KR 인코딩으로 변환하여 요청 전송
      byte[] responseBytes = restClient.post()
          .uri(url)
          .contentType(org.springframework.http.MediaType.APPLICATION_XML)
          .body(xmlBody.getBytes(Charset.forName("EUC-KR")))
          .retrieve()
          .body(byte[].class);

      if (responseBytes == null) {
        return Optional.empty();
      }

      // 응답받은 byte 배열을 다시 EUC-KR 문자열로 안전하게 디코딩
      String responseXml = new String(responseBytes, Charset.forName("EUC-KR"));

      // 검색 결과 확인용 로그 (나중에 지워도 됩니다)
      log.info("🔍 [11번가 검색 응답] SKU: {} -> \n{}", sku, responseXml);

      // 🚀 복잡한 XML 파싱 대신, 정규식으로 핵심 상품번호(<prdNo> 숫자 </prdNo>)만 빠르게 낚아챕니다!
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<prdNo>(\\d+)</prdNo>");
      java.util.regex.Matcher matcher = pattern.matcher(responseXml);

      if (matcher.find()) {
        String prdNo = matcher.group(1);
        log.info("🎯 [11번가 검색 성공] SKU: {} -> prdNo: {}", sku, prdNo);
        return Optional.of(prdNo);
      } else {
        log.warn("🔍 [검색 실패] 11번가 SKU: {} -> 응답에서 <prdNo>를 찾을 수 없습니다.", sku);
      }

    } catch (Exception e) {
      log.error("❌ 11번가 SKU({}) 검색 통신 실패: {}", sku, e.getMessage());
    }*/

    return Optional.of(sku);
  }

  // =========================================================================
  // 2. Extractor Port: 11번가 데이터 추출 (🚀 정찰기 가동)
  // =========================================================================
  @Override
  public MarketExtractedData extractInitialProductData(String marketProductNo) {
    // 11번가 상품 상세 조회 오픈 API (보통 OpenAPI 방식 사용)
    // String url = "http://openapi.11st.co.kr/openapi/OpenApiService.tmall?key="
    //     + properties.getApiKey() + "&apiCode=ProductInfo&productCode=" + marketProductNo;

    // 🚀 일반 OpenAPI가 아닌, 판매자 전용(Seller) API 엔드포인트를 호출합니다!
    String url = properties.getApiUrl() + "/rest/prodservices/product/" + marketProductNo;

    try {
      // 🚀 한글 깨짐 방지: byte 배열로 받은 뒤 EUC-KR로 강제 변환!
      // Header에 openapikey는 어댑터 생성자에서 이미 세팅해두었습니다!
      byte[] responseBytes = restClient.get()
          .uri(url)
          .retrieve()
          .body(byte[].class);

      String xmlResponse = new String(responseBytes, Charset.forName("EUC-KR"));

      log.info("==================================================");
      log.info("📦 [11번가 탐색] 원본 상품번호: {}", marketProductNo);
      log.info("==================================================");
      log.info("\n{}", xmlResponse); // 🚀 이번엔 엄청나게 긴 진짜 XML이 쏟아질 겁니다!
      log.info("==================================================");

      // 11번가도 우리의 마스터 데이터가 아니므로 덮어쓰기 방지
      return MarketExtractedData.builder()
          .isMasterData(false)
          .marketIdentifiers(Map.of("elevenstProductNo", marketProductNo))
          .name("11번가 정찰중")
          .originalName("")
          .salePrice(BigDecimal.ZERO)
          .stock(0)
          .detailHtml("탐색 완료")
          .images(new ArrayList<>())
          .rawData(new HashMap<>()) // 나중에 XML을 Map으로 변환해서 넣어야 함
          .build();

    } catch (Exception e) {
      log.error("❌ 11번가 상품 탐색 실패 ({}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("11번가 파싱 중 오류 발생", e);
    }
  }

  // =========================================================================
  // 3. Command Port: 상태 변경 및 마킹 (일단 껍데기)
  // =========================================================================
  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    log.info("📝 [11번가 Command] 메모 업데이트 껍데기 호출됨 - 상품번호: {}", marketProductNo);
  }
}