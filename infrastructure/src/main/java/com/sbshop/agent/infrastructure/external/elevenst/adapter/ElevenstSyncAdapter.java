package com.sbshop.agent.infrastructure.external.elevenst.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.ELEVENST;
  }

  @Override
  public List<String> fetchAllMarketItemIds() {

    List<String> allIds = new ArrayList<>();

    // 11번가 명세서에 맞춰 limit를 500 이하의 안전한 값으로 설정 (예: 100)
    int limit = 100;
    // 11번가는 페이징 시 주로 start(인덱스) 또는 page를 사용합니다. (명세서의 SearchProduct 내부 파라미터 확인 필수!)
    int start = 1;
    boolean hasMore = true;

    // 🚀 XML을 우리가 익숙한 JsonNode 트리 구조로 바꿔주는 마법사!
    XmlMapper xmlMapper = new XmlMapper();

    log.info("🚀 [11번가] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {

        // 11번가 다중 상품 조회 URL
        String path = "/rest/prodmarketservice/prodmarket";

        // 🚀 1. 11번가 API 스펙에 맞춘 XML 페이로드 생성 (문자열 조합)
        // 실제 API 가이드의 <SearchProduct> 하위 노드명(limit, start, selStatCd 등)과 정확히 일치시켜야 합니다!
        String xmlBody = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SearchProduct>\n" +
                "    <limit>%d</limit>\n" +
                "    <start>%d</start>\n" +
                "    \n" +
                "</SearchProduct>", limit, start);

        // 🚀 2. XML로 쏘고 XML로 받기
        String responseXml = elevenstRestClient.postXml(path, xmlBody);

        // 🚀 3. 받은 XML을 JsonNode처럼 트리로 변환!
        JsonNode rootNode = xmlMapper.readTree(responseXml);

        // 4. 상품 리스트 노드 찾기 (XML은 보통 반복되는 태그 이름인 <product> 로 바로 접근합니다)
        // 11번가 응답이 <products><product>...</product></products> 라면 "product"를 찾습니다.
        JsonNode productNode = rootNode.path("product");

        if (productNode.isMissingNode() || productNode.isEmpty()) {
          hasMore = false;
        } else {
          // 🚀 [중요] Jackson XML 파서의 특징 방어:
          // 결과가 2개 이상이면 '배열'로, 딱 1개면 '단일 객체'로 인식합니다.
          if (productNode.isArray()) {
            for (JsonNode node : productNode) {
              String prdNo = node.path("prdNo").asText("");
              String selStatCd = node.path("selStatCd").asText(""); // 🚀 상태 코드 추출!
              String selStatNm = node.path("selStatNm").asText(""); // 상태명 (로깅용)

              // 🚀 [핵심 방어벽] 강제판매종료(106) 등 살릴 수 없는 좀비 상품은 바구니에 담지 않고 버립니다!
              if ("106".equals(selStatCd) || "107".equals(selStatCd) || "108".equals(selStatCd)) {
                log.warn("   🧟 [11번가] 제어 불가 좀비 상품 입구 컷! (ID: {}, 상태: {})", prdNo, selStatNm);
                continue; // 다음 상품으로 스킵!
              }

              if (!prdNo.isBlank()) {
                allIds.add(prdNo);
              }
            }
          } else {
            // 상품이 1개뿐일 때
            String prdNo = productNode.path("prdNo").asText("");
            if (!prdNo.isBlank()) {
              allIds.add(prdNo);
            }
          }

          // 다음 페이지 세팅 (명세서가 start가 인덱스 증가 방식인지, 페이지 증가 방식인지 확인하여 수정)
          start += limit; // 인덱스 방식이라면
          // start++; // 페이지 방식이라면

          Thread.sleep(300); // 11번가 API Rate Limit 방어
        }
      } catch (Exception e) {
        log.error("❌ 11번가 상품 목록 조회 중 치명적 오류 (start: {}): {}", start, e.getMessage());
        throw new RuntimeException("11번가 API 장애로 동기화를 강제 중단합니다.", e);
      }
    }
    log.info("📦 [11번가] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketExtractedData extractProductData(String marketItemId) { // marketProductId = prdNo
    // 1. 단건 상세 조회 엔드포인트 호출 (GET)
    String path = "/rest/prodmarketservice/prodmarket/" + marketItemId;

    // 🚀 (주의) 11번가가 EUC-KR로 응답을 줄 수 있으므로, RestClient의 get()도 EUC-KR 디코딩을 지원하도록 세팅되어야 안전합니다.
    String responseXml = elevenstRestClient.get(path);

    try {
      // 🚀 2. XML 응답을 JsonNode 트리 구조로 변환
      XmlMapper xmlMapper = new XmlMapper();
      JsonNode rootNode = xmlMapper.readTree(responseXml);

      // 11번가 XML의 최상단 루트 태그(<Product>)가 rootNode 자체가 됩니다.
      // 🚀 11번가 자체 SKU 필드명: sellerPrdCd
      String mappingKey = rootNode.path("sellerPrdCd").asText("");
      String name = rootNode.path("prdNm").asText("");
      String detailHtml = rootNode.path("prdDetail").asText("");

      return MarketExtractedData.builder()
          .isMasterData(true)
          .mappingKey(mappingKey) // 핵심 매핑 키!
          .marketIdentifiers(dataMapper.buildIdentifiers(marketItemId, rootNode))
          // .name(name)
          // .detailHtml(detailHtml)
          // .images(imageExtractor.extractSkuImages(detailHtml, mappingKey))
          // XML -> Map 변환
          .rawData(objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {}))
          .build();

    } catch (Exception e) {
      log.error("❌ [11번가] 상품 정보 추출 실패 (ID: {}): {}", marketItemId, e.getMessage());
      throw new RuntimeException("11번가 데이터 추출 오류", e);
    }
  }

  @Override
  public boolean deleteMarketProduct(String marketItemId) {
    // 11번가는 하드 딜리트(DELETE)가 사실상 불가능하므로 '판매중지' 전용 API를 찔러서 즉시 상품을 내립니다.
    log.info("   👻 [11번가] 유령 상품 발견! 상품을 즉시 '판매중지' 상태로 변경합니다. (ID: {})", marketItemId);

    try {
      // 🚀 11번가 상태 변경 전용 엔드포인트 (판매중지: /stat/stop/)
      String path = "/rest/prodstatusservice/stat/stop/" + marketItemId;

      // 🚀 상태 변경용 아주 가벼운 XML 페이로드 (문서에 따라 ProductStat 혹은 단순 요청일 수 있습니다)
      String xmlBody = String.format(
          "<?xml version=\"1.0\" encoding=\"euc-kr\" standalone=\"yes\"?>\n" +
              "<ProductStat>\n" +
              "    <prdNo>%s</prdNo>\n" +
              "</ProductStat>", marketItemId);

      // API 쏘기!
      elevenstRestClient.postXml(path, xmlBody);

      // ====================================================================
      log.info("   🛑 [11번가] 판매중지 처리 성공! (ID: {})", marketItemId);
      log.info("   👉 11번가 셀러오피스의 [판매중지] 탭에서 이 상품들을 모아 한 번에 일괄 삭제하시면 됩니다.");
      // ====================================================================

      // 성공적으로 중지시켰으므로 우리 로컬 DB의 찌꺼기를 정리할 수 있도록 true를 반환!
      return true;

    } catch (Exception e) {
      log.error("   ❌ [11번가] 유령 상품 판매중지 처리 실패 (ID: {}): {}", marketItemId, e.getMessage());

      // 최후의 보루: 로그에 강력하게 마킹
      log.error(" 🚨 [수동 조치 요망] 11번가 셀러오피스에 접속하여 수동으로 중지/삭제해 주세요. 👉 상품번호: {}", marketItemId);

      return false;
    }
  }
}
