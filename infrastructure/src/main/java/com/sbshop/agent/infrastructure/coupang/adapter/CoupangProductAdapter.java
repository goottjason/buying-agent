package com.sbshop.agent.infrastructure.coupang.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketProductDto;
// (기존에 쓰시던 CoupangApiUtil 또는 CoupangWebClient를 주입받는다고 가정합니다)
import com.sbshop.agent.infrastructure.coupang.client.CoupangWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangProductAdapter implements MarketProductPort {

  private final CoupangWebClient webClient; // 기존 executeRequest 로직을 감싼 클라이언트
  private final ObjectMapper objectMapper;

  // 🚀 [핵심 1] 팩토리가 나를 '쿠팡' 담당으로 인식하게 합니다!
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  @Override
  public Optional<String> findProductNoBySku(String sku) {
    // 쿠팡 API: 판매자 상품 코드(sellerProductName 또는 externalVendorSku)로 상품 조회
    // 참고: 쿠팡은 목록 조회 API 결과에서 sellerProductId를 추출해야 합니다.
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products?sellerProductName=" + sku;

    try {
      String responseJson = webClient.get(path); // 내부적으로 쿠팡 HMAC 서명 통신
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode dataNode = root.path("data");

      if (dataNode.isArray() && dataNode.size() > 0) {
        // 첫 번째 검색된 상품의 sellerProductId를 반환
        return Optional.of(dataNode.get(0).path("sellerProductId").asText());
      }
    } catch (Exception e) {
      log.error("쿠팡 SKU({}) 검색 파싱 실패: {}", sku, e.getMessage());
    }
    return Optional.empty();
  }

  @Override
  public MarketProductDto getProductDetails(String marketProductNo) {
    // 기존 레거시 코드의 findProductInfo() 활용
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductNo;

    try {
      String responseJson = webClient.get(path);
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode dataNode = root.path("data");

      // 1. 상세 HTML 추출 (쿠팡은 vendorItems -> contents 안에 복잡하게 들어있습니다)
      String detailHtml = "";
      JsonNode items = dataNode.path("items");
      if (items.isArray() && items.size() > 0) {
        JsonNode contents = items.get(0).path("contents");
        if (contents.isArray() && contents.size() > 0) {
          JsonNode contentDetails = contents.get(0).path("contentDetails");
          if (contentDetails.isArray() && contentDetails.size() > 0) {
            detailHtml = contentDetails.get(0).path("content").asText("");
          }
        }
      }

      // 2. 이미지 추출 (displayImages)
      List<String> images = new ArrayList<>();
      JsonNode displayImages = dataNode.path("displayCategoryCode"); // 혹은 items 내부의 images
      // (쿠팡 JSON 구조에 맞게 실제 이미지 경로 파싱 로직 작성)
      // images.add(...);

      // 🚀 [핵심 2] 쿠팡의 복잡한 JSON을 까서 우리가 약속한 공통 DTO로 예쁘게 포장해 줍니다!
      return MarketProductDto.builder()
          .detailHtml(detailHtml)
          .images(images)
          .build();

    } catch (Exception e) {
      log.error("쿠팡 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("쿠팡 상품 파싱 중 오류 발생");
    }
  }

  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 주의: 쿠팡은 Cafe24처럼 상품에 간단한 '메모'만 수정하는 API가 없습니다.
    // 수정하려면 상품 전체 스키마를 던져서 PUT을 해야 하거나,
    // 옵션의 판매자 상품코드(externalVendorSku) 등을 수정해야 합니다.
    // 동기화 목적이라면, 여기서는 별도 로직을 비워두거나 내부 DB 연동 기록만으로 갈음할 수 있습니다.

    log.info("쿠팡은 단일 메모 수정 API를 지원하지 않습니다. (내부 DB MarketRegistration에 동기화 기록 완료) - 상품: {}", marketProductNo);
  }
}