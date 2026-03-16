package com.sbshop.agent.infrastructure.client.elevenst.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import com.sbshop.agent.infrastructure.client.common.util.HtmlImageExtractor;
import com.sbshop.agent.infrastructure.client.elevenst.client.ElevenstRestClient;
import com.sbshop.agent.infrastructure.client.elevenst.mapper.ElevenstDataMapper;
import com.sbshop.agent.infrastructure.client.elevenst.parser.ElevenstProductParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstMarketClient implements MarketClient {

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
  public Map<String, String> publish(Product product) {
    return Map.of();
  }

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
  public MarketItemInfo extractMarketItem(String marketItemId) { // marketProductId = prdNo
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

      return MarketItemInfo.builder()
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
  public MarketItemInfo parseLocalData(Map<String, Object> rawData) {
    if (rawData == null || rawData.isEmpty()) {
      return MarketItemInfo.builder().build();
    }

    // =====================================================================
    // 1. 1차원 Flat 데이터 추출 (상품명, SKU)
    // =====================================================================
    String name = rawData.get("prdNm") != null ? String.valueOf(rawData.get("prdNm")) : null;
    String mappingKey = rawData.get("sellerPrdCd") != null ? String.valueOf(rawData.get("sellerPrdCd")) : "";

    // =====================================================================
    // 2. 가격 및 재고 파싱 (안전하게 처리)
    // =====================================================================
    BigDecimal salePrice = null;
    if (rawData.get("selPrc") != null) {
      try {
        salePrice = new BigDecimal(String.valueOf(rawData.get("selPrc")));
      } catch (NumberFormatException e) {
        log.warn("11번가 로컬 데이터 가격 파싱 실패: {}", rawData.get("selPrc"));
      }
    }

    Integer stock = 0;
    if (rawData.get("prdStckQty") != null) {
      try {
        stock = Integer.parseInt(String.valueOf(rawData.get("prdStckQty")));
      } catch (NumberFormatException e) {
        log.warn("11번가 로컬 데이터 재고 파싱 실패: {}", rawData.get("prdStckQty"));
      }
    }

    // =====================================================================
    // 3. 조립 및 반환
    // =====================================================================
    return MarketItemInfo.builder()
        .isMasterData(true)
        .name(name)
        .mappingKey(mappingKey)
        // 💡 11번가 기본 상품조회 API 응답에는 명시적인 브랜드 필드가 없으므로 생략합니다.
        .salePrice(salePrice)
        .stock(stock)
        .rawData(rawData)
        .build();
  }

  @Override
  public Map<String, Object> syncPriceAndStock(String marketItemId, Map<String, Object> currentRawData, Integer price, Integer stock) {

    // 🚀 1. 11번가 오픈 API 호출 로직
    // elevenstRestClient.updatePriceAndStock(marketItemId, price, stock);

    // 🚀 2. 로컬 Map 패치 (11번가는 Flat 구조라 가장 간단합니다!)
    try {
      if (currentRawData != null) {
        // 11번가 스펙에 맞게 String으로 감싸서 덮어쓰기
        if (price != null) currentRawData.put("selPrc", String.valueOf(price));
        if (stock != null) currentRawData.put("prdStckQty", String.valueOf(stock));
      }
    } catch (Exception e) {
      log.warn("11번가 로컬 Map 데이터 패치 중 오류 발생", e);
    }

    return currentRawData;
  }

  @Override
  public Map<String, Object> syncImagesAndHtml(String marketItemId, Map<String, Object> currentRawData, List<String> hostedImages, String newDetailHtml) {

    // =====================================================================
    // 1. 매뉴얼 원칙 준수: 현재 11번가 서버에 등록된 "전체 XML 전문"을 그대로 조회해옵니다.
    // =====================================================================
    String currentXml;
    try {
      //     String path = "/rest/prodmarketservice/prodmarket/" + marketItemId;
      currentXml = elevenstRestClient.get("/rest/prodmarketservice/prodmarket/" + marketItemId);
      if (currentXml == null || currentXml.isEmpty()) {
        throw new RuntimeException("11번가 기존 상품 XML 조회 실패");
      }
    } catch (Exception e) {
      log.error("11번가 상품 조회 에러", e);
      throw new RuntimeException("11번가 XML 전문 조회 실패", e);
    }
    log.info("{}", currentXml);

    // =====================================================================
    // 2. 조회된 원본 XML에서 이미지와 HTML 부분만 정규식으로 정교하게 갈아끼웁니다.
    // =====================================================================
    String updatedXml = currentXml;

    // (1) 상세설명 HTML 치환
    // 방화벽(SK Planet) 입구컷 에러(409) 방지를 위해 esmplus 링크를 https로 강제 변환
    String safeHtml = newDetailHtml.replace("http://ai.esmplus.com", "https://ai.esmplus.com");

    // 정규식을 이용해 기존 <htmlDetail> 태그 전체를 새 내용으로 덮어씁니다.
    // Matcher.quoteReplacement를 써야 HTML 안의 특수문자로 인한 정규식 에러를 막을 수 있습니다.
    updatedXml = updatedXml.replaceAll("(?s)<htmlDetail>.*?</htmlDetail>",
        "<htmlDetail><![CDATA[" + java.util.regex.Matcher.quoteReplacement(safeHtml) + "]]></htmlDetail>");

    // (2) 이미지 치환
    if (hostedImages != null && !hostedImages.isEmpty()) {
      // 대표 이미지 (prdImage01)
      updatedXml = updatedXml.replaceAll("(?s)<prdImage01>.*?</prdImage01>",
          "<prdImage01><![CDATA[" + hostedImages.get(0) + "]]></prdImage01>");

      // 추가 이미지 (prdImage02 ~ 05)
      for (int i = 1; i < hostedImages.size() && i <= 4; i++) {
        String tagName = "prdImage0" + (i + 1);
        String newTag = "<" + tagName + "><![CDATA[" + hostedImages.get(i) + "]]></" + tagName + ">";

        if (updatedXml.contains("<" + tagName + ">")) {
          // 기존에 해당 태그가 있다면 치환
          updatedXml = updatedXml.replaceAll("(?s)<" + tagName + ">.*?</" + tagName + ">", newTag);
        } else {
          // 기존에 추가 이미지가 없었다면 대표 이미지 태그 바로 뒤에 새로 삽입
          updatedXml = updatedXml.replace("</prdImage01>", "</prdImage01>\n  " + newTag);
        }
      }
    }

    // 🚀 =====================================================================
    // (3) [신규] 11번가 단골 에러 방지를 위한 누락 데이터 강제 주입!
    // =====================================================================

    // 1. 발송택배사 강제 지정 (CJ대한통운: 00034)
    if (updatedXml.contains("<dlvEtprsCd>")) {
      updatedXml = updatedXml.replaceAll("(?s)<dlvEtprsCd>.*?</dlvEtprsCd>", "<dlvEtprsCd>00034</dlvEtprsCd>");
    } else {
      updatedXml = updatedXml.replace("</Product>", "  <dlvEtprsCd>00034</dlvEtprsCd>\n</Product>");
    }

    // 2. 원재료 유형 코드 및 상세설명 강제 지정
    String rmaterialXml = "<rmaterialTypCd>03</rmaterialTypCd>\n" +
        "  <ProductRmaterial>\n" +
        "    <rmaterialNm><![CDATA[상세설명 참조]]></rmaterialNm>\n" +
        "    <ingredNm><![CDATA[상세설명 참조]]></ingredNm>\n" +
        "    <orgnCountry><![CDATA[상세설명 참조]]></orgnCountry>\n" +
        "    <content><![CDATA[상세설명 참조]]></content>\n" +
        "  </ProductRmaterial>";

    if (updatedXml.contains("<rmaterialTypCd>")) {
      updatedXml = updatedXml.replaceAll("(?s)<rmaterialTypCd>.*?</rmaterialTypCd>", "<rmaterialTypCd>03</rmaterialTypCd>");
      if (!updatedXml.contains("<ProductRmaterial>")) {
        updatedXml = updatedXml.replace("</Product>",
            "  <ProductRmaterial>\n" +
                "    <rmaterialNm><![CDATA[상세설명 참조]]></rmaterialNm>\n" +
                "    <ingredNm><![CDATA[상세설명 참조]]></ingredNm>\n" +
                "    <orgnCountry><![CDATA[상세설명 참조]]></orgnCountry>\n" +
                "    <content><![CDATA[상세설명 참조]]></content>\n" +
                "  </ProductRmaterial>\n</Product>");
      }
    } else {
      updatedXml = updatedXml.replace("</Product>", "  " + rmaterialXml + "\n</Product>");
    }
    // 🚀 [추가] 3. 판매방식 강제 지정 (고정가판매: 01)
    if (updatedXml.contains("<selMthdCd>")) {
      updatedXml = updatedXml.replaceAll("(?s)<selMthdCd>.*?</selMthdCd>", "<selMthdCd>01</selMthdCd>");
    } else {
      updatedXml = updatedXml.replace("</Product>", "  <selMthdCd>01</selMthdCd>\n</Product>");
    }

    // 🚀 [추가] 4. 배송/반품 관련 필수값 "일괄" 강제 주입 (숨바꼭질 종결!)
    // 11번가는 배송비 설정 코드가 들어가면 반품비, 교환비, AS안내까지 연쇄적으로 요구합니다.
    if (!updatedXml.contains("<dlvCstInstBasiCd>")) {
      updatedXml = updatedXml.replace("</Product>",
          "  <dlvCstInstBasiCd>01</dlvCstInstBasiCd>\n" + // 02: 고정배송비 (무료일 경우 01)
              "  <dlvCstPayTypCd>03</dlvCstPayTypCd>\n" +     // 03: 선결제
              "  <bndlDlvCnYn>N</bndlDlvCnYn>\n" +            // 묶음배송 불가
              "  <rtngdDlvCst>7000</rtngdDlvCst>\n" +         // 편도 반품 배송비
              "  <exchDlvCst>7000</exchDlvCst>\n" +           // 왕복 교환 배송비
              "  <asDetail><![CDATA[상품 상세설명 참조]]></asDetail>\n" + // A/S 안내 (필수)
              "  <rtngExchDetail><![CDATA[상품 상세설명 참조]]></rtngExchDetail>\n" + // 반품/교환 안내 (필수)
              "</Product>");
    }

    // 🚀 [추가] 5. 출고지/반품지 주소 시퀀스 코드 및 해외 여부 강제 주입
    // 주의: "123456"은 임시 번호입니다. 아래 💡확인 방법을 참고하여 종원님의 실제 시퀀스 번호로 변경해 주세요!
    String defaultAddrSeq = "5"; // TODO: 실제 11번가 출고지/반품지 시퀀스 번호 입력!

    // 출고지 주소 시퀀스
    if (!updatedXml.contains("<addrSeqOut>")) {
      updatedXml = updatedXml.replace("</Product>", "  <addrSeqOut>" + "5" + "</addrSeqOut>\n</Product>");
    }
    // 반품지 주소 시퀀스
    if (!updatedXml.contains("<addrSeqIn>")) {
      updatedXml = updatedXml.replace("</Product>", "  <addrSeqIn>" + "2" + "</addrSeqIn>\n</Product>");
    }

    // 국내 배송 여부 (Y: 해외, N: 국내)
    if (!updatedXml.contains("<outsideYnOut>")) {
      updatedXml = updatedXml.replace("</Product>", "  <outsideYnOut>N</outsideYnOut>\n</Product>");
    }
    if (!updatedXml.contains("<outsideYnIn>")) {
      updatedXml = updatedXml.replace("</Product>", "  <outsideYnIn>N</outsideYnIn>\n</Product>");
    }

    log.info("update: {}", updatedXml);
    // =====================================================================
    // 3. API 통신: 치환이 완료된 "전체 XML 전문"을 PUT으로 전송!
    // =====================================================================
    try {
      // elevenstRestClient.requestWithBody("PUT", "/openapi/products/" + marketItemId, updatedXml);
      String responseXml = elevenstRestClient.requestWithBody("PUT", "/rest/prodservices/product/" + marketItemId, updatedXml);
      // 🚀 11번가의 진짜 속마음(결과)을 로그에 찍어봅니다!
      log.info("11번가 서버 실제 응답결과: {}", responseXml);

      // 11번가의 성공 코드는 <resultCode>200</resultCode> 또는 <resultCode>210</resultCode> 입니다.
      if (responseXml != null && !responseXml.contains("<resultCode>200</resultCode>") && !responseXml.contains("<resultCode>210</resultCode>")) {
        log.error("11번가 HTTP는 성공했으나, 내부 로직 실패로 반려됨: {}", responseXml);
        throw new RuntimeException("11번가 내부 로직 실패");
      }

      log.info("11번가 전체 XML 동기화(이미지/HTML 덮어쓰기) 완벽 성공!: {}", marketItemId);

    } catch (Exception e) {
      log.error("11번가 이미지/HTML(XML 전문) 동기화 중 에러 발생", e);
      throw new RuntimeException("11번가 동기화 실패", e);
    }

    // =====================================================================
    // 4. 로컬 데이터 패치
    // =====================================================================
    if (currentRawData != null) {
      currentRawData.put("htmlDetail", newDetailHtml);
      if (hostedImages != null && !hostedImages.isEmpty()) {
        currentRawData.put("prdImage01", hostedImages.get(0));
      }
    }
    return currentRawData;
  }

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

  public void updateProductImageAndHtml(Map<String, String> identifiers, Product product) {

  }
}
