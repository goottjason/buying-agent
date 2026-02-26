package com.sbshop.agent.infrastructure.external.smartstore.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketSyncPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.smartstore.client.SmartstoreRestClient;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreSyncAdapter implements MarketSyncPort {

  private final ObjectMapper objectMapper;
  private final SmartstoreRestClient smartstoreRestClient;

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.SMARTSTORE;
  }

  @Override
  public List<String> fetchAllMarketProductIds() {
    List<String> allIds = new ArrayList<>();
    int page = 1;
    int size = 50; // 커머스 API 권장 사이즈
    boolean hasMore = true;

    log.info("🚀 [스마트스토어] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 스마트스토어 커머스 API 상품 검색 엔드포인트
        String path = "/v1/products/search?page=" + page + "&size=" + size;

        String responseJson = smartstoreRestClient.get(path);
        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode contentNode = rootNode.path("contents");

        if (contentNode.isEmpty()) {
          hasMore = false;
        } else {
          for (JsonNode node : contentNode) {
            String originProductNo = node.path("originProductNo").asText();
            if (originProductNo != null && !originProductNo.isBlank()) {
              allIds.add(originProductNo);
            }
          }
          page++; // 다음 페이지
          Thread.sleep(300);
        }
      } catch (Exception e) {
        log.error("❌ 스마트스토어 상품 목록 조회 중 오류 (page: {}): {}", page, e.getMessage());
        break;
      }
    }
    log.info("📦 [스마트스토어] 총 {}개의 상품 ID 수집 완료!", allIds.size());
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
