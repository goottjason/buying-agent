package com.sbshop.agent.infrastructure.external.elevenst.adapter;

import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.port.MarketProductPort;
import com.sbshop.agent.core.domain.product.port.dto.MarketExtractedData;
import com.sbshop.agent.infrastructure.external.elevenst.client.ElevenstWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenstProductAdapter implements MarketProductPort {

  private final ElevenstWebClient webClient;
  // XML 파싱을 위해 기존에 쓰시던 컨버터나 XmlMapper를 주입받아도 됩니다.

  // 🚀 [핵심 1] 팩토리에게 "나는 11번가 담당이야!" 라고 외칩니다.
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.ELEVENST; // Enum에 ELEVENST 추가 필요
  }

  @Override
  public Optional<String> findMarketProductNoBySku(String sku) {
    // 11번가 셀러 상품번호(SellerPrdNo)로 상품 검색 OpenAPI 호출
    String path = "/prodstatusservice/stat/search?sellerPrdNo=" + sku;

    try {
      String xmlResponse = webClient.get(path);

      // XML에서 <ProductNo>123456</ProductNo> 추출 (예시: 정규식 또는 XmlMapper 사용)
      if (xmlResponse.contains("<ProductNo>")) {
        String productNo = xmlResponse.split("<ProductNo>")[1].split("</ProductNo>")[0];
        return Optional.of(productNo);
      }
    } catch (Exception e) {
      log.error("11번가 SKU({}) 검색 실패: {}", sku, e.getMessage());
    }
    return Optional.empty();
  }

  @Override
  public MarketExtractedData getProductDetailsByMarketProductNo(String marketProductNo) {
    // 11번가 단일 상품 조회 API
    String path = "/prodservices/product/" + marketProductNo;

    try {
      String xmlResponse = webClient.get(path);

      // 1. 상세 HTML 추출 (<DetailInfo> 태그 등)
      String detailHtml = "";
      if (xmlResponse.contains("<ProductDetail>")) {
        detailHtml = xmlResponse.split("<ProductDetail>")[1].split("</ProductDetail>")[0];
        // XML CDATA 처리 등 필요
      }

      // 2. 이미지 추출 (<Image> 태그 등)
      List<String> images = new ArrayList<>();
      if (xmlResponse.contains("<BasicImage>")) {
        images.add(xmlResponse.split("<BasicImage>")[1].split("</BasicImage>")[0]);
      }

      // 🚀 [핵심 2] 더러운 XML을 파싱해서 도메인이 사랑하는 깔끔한 DTO로 포장!
      return MarketExtractedData.builder()
          .detailHtml(detailHtml)
          .images(images)
          .build();

    } catch (Exception e) {
      log.error("11번가 상품 정보 파싱 실패 (ID: {}): {}", marketProductNo, e.getMessage());
      throw new RuntimeException("11번가 상품 파싱 중 오류 발생");
    }
  }

  @Override
  public void updateSyncMemo(String marketProductNo, String syncMessage) {
    // 11번가도 쿠팡처럼 단순 메모만 업데이트하는 API는 지원하지 않으므로 패스하거나 로컬 DB 기록으로 대체합니다.
    log.info("11번가는 단일 메모 수정 API를 지원하지 않습니다. (내부 DB 기록으로 대체) - 상품: {}", marketProductNo);
  }
}