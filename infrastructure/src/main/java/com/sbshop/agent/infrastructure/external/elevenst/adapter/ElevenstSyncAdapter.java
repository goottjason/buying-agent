package com.sbshop.agent.infrastructure.external.elevenst.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.common.util.HtmlImageExtractor;
import com.sbshop.agent.infrastructure.external.elevenst.client.ElevenstRestClient;
import com.sbshop.agent.infrastructure.external.elevenst.client.ElevenstWebClient;
import com.sbshop.agent.infrastructure.external.elevenst.mapper.ElevenstDataMapper;
import com.sbshop.agent.infrastructure.external.elevenst.parser.ElevenstProductParser;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstSyncAdapter implements MarketSyncPort {

  private final ElevenstRestClient elevenstRestClient;
  private final ElevenstProductParser productParser;
  private final ElevenstDataMapper dataMapper;
  private final HtmlImageExtractor imageExtractor;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.ELEVENST;
  }

  @Override
  public List<String> fetchAllMarketProductIds() {
    List<String> allIds = new ArrayList<>();
    int pageNum = 1;
    // 11번가 검색 API는 한 번에 최대 100~200개를 권장합니다. (API 스펙에 맞춰 조정 가능)
    int pageSize = 100;
    boolean hasMore = true;

    log.info("🚀 [11번가] 전체 상품 ID 싹쓸이 시작...");

    try {
      // XML 파싱을 위한 공장(Factory) 초기 세팅 (반복문 밖에서 한 번만 생성하여 성능 최적화)
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      while (hasMore) {
        // 11번가 셀러 상품 조회 엔드포인트 (API 문서의 apiCode 확인 필요, 보통 ProductSearch 사용)
        String path = "/openapi/OpenApiService.tmall?apiCode=ProductSearch&page=" + pageNum + "&pageSize=" + pageSize;

        // 개발자님의 완벽한 클라이언트로 EUC-KR 해결된 XML 응답을 받습니다.
        String responseXml = elevenstRestClient.get(path);

        // 🚀 자바 기본 DOM 파서를 이용한 깔끔한 XML 파싱
        Document document = builder.parse(new InputSource(new StringReader(responseXml)));

        // 11번가의 상품 번호 태그는 주로 <ProductNo> 입니다. (버전에 따라 <prdNo>일 수 있으니 실제 응답 확인 필요)
        NodeList productNoNodes = document.getElementsByTagName("ProductNo");

        if (productNoNodes.getLength() == 0) {
          hasMore = false; // 더 이상 조회된 상품 태그가 없으면 탈출!
        } else {
          for (int i = 0; i < productNoNodes.getLength(); i++) {
            // <ProductNo>1234567</ProductNo> 안의 텍스트만 쏙 빼서 리스트에 담습니다.
            allIds.add(productNoNodes.item(i).getTextContent());
          }
          pageNum++;
          Thread.sleep(300); // 11번가 API Rate Limit 방어 (초당 호출 제한)
        }
      }
    } catch (Exception e) {
      log.error("❌ 11번가 상품 목록 조회 중 오류 (page: {}): {}", pageNum, e.getMessage());
      // 중간에 실패하더라도 지금까지 모은 ID는 반환하도록 예외를 던지지 않고 로그만 남깁니다.
    }

    log.info("📦 [11번가] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketExtractedData extractProductData(String marketProductId) { // marketProductId = prdNo
    // 1. 11번가 단건 상세 조회 엔드포인트 호출 (apiCode=ProductInfo)
    String path = "/openapi/OpenApiService.tmall?apiCode=ProductInfo&prdNo=" + marketProductId;

    // EUC-KR 한글 깨짐이 완벽히 방어된 개발자님의 WebClient 호출!
    String responseXml = elevenstRestClient.get(path);

    try {
      // 2. 파서로 XML -> Document 객체 변환
      Document doc = productParser.parseXml(responseXml);

      // 3. 전문가들을 통한 데이터 추출
      String sku = productParser.getText(doc, "SellerPrdCd"); // 11번가는 주로 이 태그에 자체 SKU를 넣습니다.
      String detailHtml = productParser.getText(doc, "DetailWrhs"); // 상세설명 HTML이 담긴 태그

      // 🚀 지휘관(Processor)을 위한 매칭 열쇠! (우리의 자체 SKU)
      String mappingKey = sku;

      // 💡 XML 응답을 Map으로 굳이 바꿀 필요 없이, 원본을 보존합니다.
      Map<String, Object> rawDataMap = new HashMap<>();
      rawDataMap.put("xmlResponse", responseXml);

      return MarketExtractedData.builder()
          .isMasterData(true)
          .mappingKey(mappingKey) // 🚀 핵심 열쇠 주입
          .marketIdentifiers(dataMapper.buildIdentifiers(marketProductId, doc))
          .name(productParser.getText(doc, "PrdNm"))
          .originalName("") // 11번가는 영문명 필드가 딱히 없다면 빈칸 처리
          .salePrice(dataMapper.getPrice(doc))
          .stock(dataMapper.getStock(doc))
          .detailHtml(detailHtml)
          // 🚀 상세설명이 <![CDATA[ ]]> 로 감싸져 있어도 getText()가 깔끔하게 벗겨주기 때문에 유틸리티가 완벽하게 작동합니다!
          .images(imageExtractor.extractSkuImages(detailHtml, sku))
          .rawData(rawDataMap)
          .build();

    } catch (Exception e) {
      log.error("❌ 11번가 상품 정보 추출 실패 (ID: {}): {}", marketProductId, e.getMessage());
      throw new RuntimeException("11번가 데이터 추출 오류", e);
    }
  }

  @Override
  public boolean deleteMarketProduct(String marketProductId) { // marketProductId = prdNo
    try {
      // 11번가는 물리적 삭제 대신 '상품상태변경(ProductStat)' API로 '판매중지' 처리를 합니다.
      String path = "/openapi/OpenApiService.tmall?apiCode=ProductStat";

      // 🚀 상태코드 105: 판매중지 (11번가 스펙)
      String xmlBody = "<?xml version=\"1.0\" encoding=\"euc-kr\"?>\n" +
          "<ProductStat>\n" +
          "  <prdNo>" + marketProductId + "</prdNo>\n" +
          "  <prdAstatCd>105</prdAstatCd>\n" +
          "</ProductStat>";

      // 개발자님의 완벽한 EUC-KR 무기로 PUT 요청 전송!
      elevenstRestClient.requestWithBody("PUT", path, xmlBody);

      log.info("🗑️ [11번가] 유령 상품 판매중지(삭제) 처리 완료 (ID: {})", marketProductId);
      return true;
    } catch (Exception e) {
      log.error("❌ [11번가] 유령 상품 판매중지 실패 (ID: {}): {}", marketProductId, e.getMessage());
      return false;
    }
  }
}
