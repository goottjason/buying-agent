package com.sbshop.agent.infrastructure.external.elevenst.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.elevenst.client.ElevenstRestClient;
import com.sbshop.agent.infrastructure.external.elevenst.client.ElevenstWebClient;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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
  public MarketExtractedData extractProductData(String marketProductId) {
    return null;
  }

  @Override
  public boolean deleteMarketProduct(String marketProductId) {
    return false;
  }
}
