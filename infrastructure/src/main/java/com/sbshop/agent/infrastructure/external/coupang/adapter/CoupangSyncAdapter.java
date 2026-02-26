package com.sbshop.agent.infrastructure.external.coupang.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.coupang.client.CoupangRestClient;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangSyncAdapter implements MarketSyncPort {

  private final ObjectMapper objectMapper;
  private final CoupangRestClient coupangRestClient;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  @Override
  public List<String> fetchAllMarketProductIds() {
    List<String> allIds = new ArrayList<>();
    String nextToken = ""; // 쿠팡은 페이지 번호 대신 이 토큰을 물고 다녀야 합니다.
    boolean hasMore = true;

    log.info("🚀 [쿠팡] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 쿠팡 셀러 상품 목록 조회 API (maxPerPage는 100이 최대치입니다)
        String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products?maxPerPage=100";

        // 다음 페이지 토큰이 있다면 쿼리스트링에 이어 붙입니다.
        // (개발자님의 WebClient 내 HMAC 로직이 이 전체 path를 서명에 예쁘게 말아줄 것입니다!)
        if (!nextToken.isEmpty()) {
          path += "&nextToken=" + nextToken;
        }

        // 🚀 단 한 줄로 끝나는 마법! (인증, 헤더, 타임아웃 세팅이 모두 은닉됨)
        String responseJson = coupangRestClient.get(path);

        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode dataNode = rootNode.path("data");

        if (dataNode.isEmpty()) {
          hasMore = false;
        } else {
          for (JsonNode node : dataNode) {
            // 쿠팡의 마스터 식별자인 sellerProductId 추출
            allIds.add(node.path("sellerProductId").asText());
          }

          // 🚀 쿠팡 페이징의 핵심: 응답으로 온 nextToken을 갱신
          nextToken = rootNode.path("nextToken").asText("");

          // 더 이상 토큰이 내려오지 않으면 끝까지 다 조회한 것입니다.
          if (nextToken.isEmpty()) {
            hasMore = false;
          }

          Thread.sleep(300); // 쿠팡 API Rate Limit 방어 (초당 10회 등)
        }
      } catch (Exception e) {
        log.error("❌ 쿠팡 상품 목록 조회 중 오류: {}", e.getMessage());
        break; // 에러 시 무한 루프 탈출
      }
    }

    log.info("📦 [쿠팡] 총 {}개의 상품 ID 수집 완료!", allIds.size());
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
