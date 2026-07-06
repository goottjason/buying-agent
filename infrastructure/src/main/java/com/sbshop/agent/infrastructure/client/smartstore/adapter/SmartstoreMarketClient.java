package com.sbshop.agent.infrastructure.client.smartstore.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import com.sbshop.agent.infrastructure.client.common.util.HtmlImageExtractor;
import com.sbshop.agent.infrastructure.client.smartstore.client.SmartstoreRestClient;
import com.sbshop.agent.infrastructure.client.smartstore.mapper.SmartstoreDataMapper;
import com.sbshop.agent.infrastructure.client.smartstore.parser.SmartstoreProductParser;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartstoreMarketClient implements MarketClient {

  // 통신, 파싱, 매핑, 유틸 전문가들을 모두 주입받습니다.
  private final SmartstoreRestClient smartstoreRestClient;
  private final SmartstoreProductParser productParser;
  private final SmartstoreDataMapper dataMapper;
  private final HtmlImageExtractor imageExtractor;
  private final ObjectMapper objectMapper; // rawData 변환용

  @Override
  public MarketType getSupportedMarket() {
    return MarketType.SMARTSTORE;
  }

  @Override
  public Map<String, String> publish(Product product) {
    return Map.of();
  }

  public List<String> fetchAllMarketItemIds() {
    List<String> allIds = new ArrayList<>();
    int page = 1;
    int size = 50; // 커머스 API 권장 사이즈
    boolean hasMore = true;

    log.info("🚀 [스마트스토어] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 🚀 1. GET 파라미터가 아니라 POST JSON 바디를 구성합니다.
        String path = "/v1/products/search";
        String requestBody = String.format("{\"page\": %d, \"size\": %d}", page, size);

        String responseJson = smartstoreRestClient.post(path, requestBody);
        JsonNode rootNode = objectMapper.readTree(responseJson);

        // API 응답 스펙에 따라 'contents' 혹은 'content' 일 수 있으니,
        // 혹시 데이터가 안 들어오면 이 문자열을 가장 먼저 의심해 보세요!
        JsonNode contentNode = rootNode.path("contents");

        if (contentNode.isMissingNode() || contentNode.isEmpty()) {
          hasMore = false;
        } else {
          for (JsonNode node : contentNode) {
            String originProductNo = node.path("originProductNo").asText("");
            // 🚀 빈 문자열("")까지 확실하게 방어
            if (!originProductNo.isBlank()) {
              allIds.add(originProductNo);
            }
          }
          page++; // 다음 페이지
          Thread.sleep(300);
        }
      } catch (Exception e) {
        log.error("❌ 스마트스토어 상품 목록 조회 중 오류 (page: {}): {}", page, e.getMessage());
        // 🚀 3. [핵심 방어벽] 여기서 빈 리스트를 반환하면 전체 꼬리표 참사가 또 일어납니다!
        // 무조건 예외를 던져서 PerfectSyncProcessor가 멈추게 만들어야 합니다.
        throw new RuntimeException("스마트스토어 API 장애로 인해 동기화를 강제 중단합니다.", e);
      }
    }
    log.info("📦 [스마트스토어] 총 {}개의 상품 ID 수집 완료!", allIds.size());
    return allIds;
  }

  @Override
  public MarketItemInfo extractMarketItem(String marketItemId) { // marketProductId = originProductNo
    // 1. 단건 상세 조회 엔드포인트 호출
    String path = "/v2/products/origin-products/" + marketItemId;
    String responseJson = smartstoreRestClient.get(path);

    try {
      // 2. 파서로 Root 진입
      JsonNode rootNode = productParser.parseRootNode(responseJson);
      JsonNode originProductNode = rootNode.path("originProduct");

      // 3. 전문가들을 통한 데이터 추출
      // 스마트스토어는 보통 sellerCustomCode1 에 우리의 자체 SKU를 넣습니다.
      String mappingKey = originProductNode.path("detailAttribute")
          .path("sellerCodeInfo")
          .path("sellerManagementCode").asText("");
      // String detailHtml = productParser.getTextFromOrigin(rootNode, "detailContent");

      // 🚀 지휘관(Processor)을 위한 매칭 열쇠! (우리의 자체 SKU)

      return MarketItemInfo.builder()
          .isMasterData(true)
          .mappingKey(mappingKey) // 🚀 핵심 열쇠 주입
          .marketIdentifiers(dataMapper.buildIdentifiers(marketItemId, rootNode))
          // .name(productParser.getTextFromOrigin(rootNode, "name"))
          // .salePrice(dataMapper.getSalePrice(rootNode))
          // .stock(dataMapper.getStockQuantity(rootNode))
          // .detailHtml(detailHtml)
          // 🚀 카페24에서 떼어낸 공통 유틸이 여기서 빛을 발합니다!
          // .images(imageExtractor.extractSkuImages(detailHtml, sku))
          .rawData(objectMapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {}))
          .build();

    } catch (Exception e) {
      log.error("❌ 스마트스토어 상품 정보 추출 실패 (ID: {}): {}", marketItemId, e.getMessage());
      throw new RuntimeException("스마트스토어 데이터 추출 오류", e);
    }
  }

  @Override
  public MarketItemInfo parseLocalData(Map<String, Object> rawData) {
    if (rawData == null || rawData.isEmpty()) {
      return MarketItemInfo.builder().build();
    }

    String name = null;
    String mappingKey = "";
    BigDecimal salePrice = null;
    Integer stock = 0;
    String brand = null;
    String manufacturer = null;

    try {
      // =====================================================================
      // 1. 최상단 originProduct 객체 진입
      // =====================================================================
      Object originProductObj = rawData.get("originProduct");
      if (originProductObj instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> originProduct = (Map<String, Object>) originProductObj;

        // 상품명, 가격, 재고 추출
        name = originProduct.get("name") != null ? String.valueOf(originProduct.get("name")) : null;

        if (originProduct.get("salePrice") != null) {
          salePrice = new BigDecimal(String.valueOf(originProduct.get("salePrice")));
        }

        if (originProduct.get("stockQuantity") != null) {
          // 숫자 파싱은 안전하게 Double을 거쳐 int로 변환
          stock = (int) Double.parseDouble(String.valueOf(originProduct.get("stockQuantity")));
        }

        // =====================================================================
        // 2. 깊숙한 detailAttribute 객체 진입 (SKU, 브랜드 정보)
        // =====================================================================
        Object detailAttributeObj = originProduct.get("detailAttribute");
        if (detailAttributeObj instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> detailAttribute = (Map<String, Object>) detailAttributeObj;

          // 매핑 키 (SKU) 추출
          Object sellerCodeInfoObj = detailAttribute.get("sellerCodeInfo");
          if (sellerCodeInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sellerCodeInfo = (Map<String, Object>) sellerCodeInfoObj;
            mappingKey = sellerCodeInfo.get("sellerManagementCode") != null
                ? String.valueOf(sellerCodeInfo.get("sellerManagementCode")) : "";
          }

          // 브랜드 및 제조사 추출
          Object searchInfoObj = detailAttribute.get("naverShoppingSearchInfo");
          if (searchInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> searchInfo = (Map<String, Object>) searchInfoObj;
            brand = searchInfo.get("brandName") != null ? String.valueOf(searchInfo.get("brandName")) : null;
            manufacturer = searchInfo.get("manufacturerName") != null ? String.valueOf(searchInfo.get("manufacturerName")) : null;
          }
        }
      }
    } catch (Exception e) {
      log.warn("스마트스토어 로컬 데이터 파싱 중 오류 발생", e);
    }

    // =====================================================================
    // 3. 조립 및 반환
    // =====================================================================
    return MarketItemInfo.builder()
        .isMasterData(true)
        .name(name)
        .mappingKey(mappingKey)
        .brand(brand)
        .manufacturer(manufacturer)
        .salePrice(salePrice)
        .stock(stock)
        .rawData(rawData)
        .build();
  }

  @Override
  public Map<String, Object> syncPriceAndStock(String marketItemId, Map<String, Object> currentRawData, Integer price, Integer stock) {

    // 🚀 1. 스마트스토어 커머스 API 호출 로직
    // smartstoreRestClient.updateProduct(marketItemId, price, stock);

    // 🚀 2. 로컬 Map 패치
    try {
      if (currentRawData != null && currentRawData.containsKey("originProduct")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> originProduct = (Map<String, Object>) currentRawData.get("originProduct");

        if (originProduct != null) {
          if (price != null) originProduct.put("salePrice", price);
          if (stock != null) originProduct.put("stockQuantity", stock);
        }
      }
    } catch (Exception e) {
      log.warn("스마트스토어 로컬 Map 데이터 패치 중 오류 발생", e);
    }

    return currentRawData;
  }

  @Override
  public Map<String, Object> syncImagesAndHtml(String marketItemId, Map<String, Object> currentRawData, List<String> hostedImages, String newDetailHtml) {

    // 🚀 [핵심 수정] 새 객체를 만들지 않고, 로컬에 저장해둔 기존 마켓 데이터를 그대로 꺼냅니다!
    @SuppressWarnings("unchecked")
    Map<String, Object> originProduct = (Map<String, Object>) currentRawData.get("originProduct");

    if (originProduct == null) {
      throw new IllegalStateException("스마트스토어 기존 데이터가 없습니다. 상품 상세에서 [최신 상태 불러오기]를 먼저 진행해주세요.");
    }

    // 🚀 [추가] 네이버 이미지 업로드 로직 (외부 링크 대신 네이버 서버로 직접 업로드)
    List<String> naverImageUrls = new ArrayList<>();
    if (hostedImages != null && !hostedImages.isEmpty()) {
      try {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (String imageUrl : hostedImages) {
          byte[] imageBytes = downloadImage(imageUrl);
          if (imageBytes != null) {
            // 네이버는 파일명이 포함된 Resource 형태를 요구합니다.
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
              @Override
              public String getFilename() {
                return "image.jpg";
              }
            };
            body.add("imageFiles", resource);
          }
        }
        
        if (!body.isEmpty()) {
          JsonNode uploadResponse = smartstoreRestClient.uploadImages(body);
          if (uploadResponse != null && uploadResponse.has("images")) {
            for (JsonNode imgNode : uploadResponse.get("images")) {
              naverImageUrls.add(imgNode.get("url").asText());
            }
            log.info("   📸 [스마트스토어] 이미지 {}개 공식 업로드 완료", naverImageUrls.size());
          }
        }
      } catch (Exception e) {
        log.warn("   ⚠️ [스마트스토어] 이미지 공식 업로드 실패, 기존 링크 방식을 시도합니다: {}", e.getMessage());
      }
    }

    // 업로드된 네이버 URL이 있으면 그것을 사용하고, 없으면 기존 R2 링크를 사용
    List<String> targetImages = naverImageUrls.isEmpty() ? hostedImages : naverImageUrls;

    // 이미지 객체 조립
    Map<String, Object> imagesObj = new HashMap<>();
    if (targetImages != null && !targetImages.isEmpty()) {
      String mainImageUrl = ensureImageExtension(targetImages.get(0));
      imagesObj.put("representativeImage", Map.of("url", mainImageUrl));
      
      if (targetImages.size() > 1) {
        List<Map<String, String>> optionalImages = new ArrayList<>();
        for (int i = 1; i < targetImages.size() && i <= 10; i++) {
          optionalImages.add(Map.of("url", ensureImageExtension(targetImages.get(i))));
        }
        imagesObj.put("optionalImages", optionalImages);
      }
    }

    // 🚀 [추가] 해외 상품인 경우 관부가세 설정 필수 대응
    @SuppressWarnings("unchecked")
    Map<String, Object> detailAttribute = (Map<String, Object>) originProduct.get("detailAttribute");
    if (detailAttribute == null) {
      detailAttribute = new HashMap<>();
      originProduct.put("detailAttribute", detailAttribute);
    }
    // 기존값 확인 후 강제 설정 (API 규격에 맞게 TAX로 테스트 시도)
    Object existingTaxType = detailAttribute.get("customsTaxType");
    log.info("   ℹ️ [스마트스토어] 기존 customsTaxType 값: [{}]", existingTaxType);
    detailAttribute.put("customsTaxType", "INCLUDED");

    // 🚀 기존 데이터 덩어리에 이미지와 HTML만 쏙 갈아끼웁니다! (나머지 필수값은 그대로 유지됨)
    originProduct.put("images", imagesObj);
    originProduct.put("detailContent", newDetailHtml);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("originProduct", originProduct);

    // 🚀 [디버그] 전송 직전 전체 JSON 구조를 로그로 출력 (customsTaxType의 실제 위치 및 타입 확인용)
    try {
      log.info("📤 [스마트스토어] 업데이트 요청 JSON: {}", objectMapper.writeValueAsString(requestBody));
    } catch (Exception e) {
      log.warn("   ⚠️ [스마트스토어] JSON 로깅 중 오류 발생", e);
    }

    // API 통신
    smartstoreRestClient.put("/v2/products/origin-products/" + marketItemId, requestBody);
    log.info("스마트스토어 원상품(v2) 이미지/HTML 동기화 완료: {}", marketItemId);

    // 로컬 객체를 직접 수정했으므로 currentRawData는 이미 최신 상태입니다.
    return currentRawData;
  }

  public boolean deleteMarketProduct(String marketItemId) {
    try {
      String path = "/v2/products/origin-products/" + marketItemId;
      smartstoreRestClient.delete(path);

      log.info("   🗑️ [스마트스토어] 유령 상품 완벽 삭제(DELETE) 완료 (ID: {})", marketItemId);
      return true;

    } catch (Exception e) {
      // 네이버 정책상 판매 이력이 있거나 승인된 상품은 삭제를 거부(400 Bad Request 등)할 확률이 높습니다.
      log.warn("   ⚠️ [스마트스토어] 유령 상품 삭제 거부됨 (ID: {}). 정책상 삭제 불가일 수 있습니다. 사유: {}", marketItemId, e.getMessage());

      // TODO: 만약 네이버가 삭제를 계속 튕겨낸다면, 쿠팡처럼 '판매 중지'로 우회하는 PUT API를 여기에 뚫어주세요!
        /*
        try {
            log.info("   👻 [스마트스토어] 삭제 대신 '판매 중지(SUSPENSION)' 우회 로직을 가동합니다.");
            String putPath = "/v1/products/" + marketItemId; // 또는 상태 변경 전용 엔드포인트
            // 상태를 SUSPENSION 이나 CLOSE 로 변경하는 JSON 바디 전송
            smartstoreRestClient.put(putPath, "{\"statusType\": \"SUSPENSION\"}");
            log.info("   🛑 [스마트스토어] 삭제 대신 판매 중지 처리 성공 (ID: {})", marketItemId);
            return true; // 로컬 DB 정리를 위해 true 반환
        } catch (Exception putEx) {
            log.error("   ❌ [스마트스토어] 판매 중지 우회 로직도 실패했습니다 (ID: {})", marketItemId, putEx);
        }
        */

      // 당장은 안전을 위해 false를 반환하여 로컬 DB에 찌꺼기를 일단 남겨둡니다.
      return false;
    }
  }

  public void updateProductImageAndHtml(Map<String, String> identifiers, Product product) {

    String originProductNo = identifiers.get("originProductNo");

    if (originProductNo == null || originProductNo.isBlank()) {
      throw new IllegalArgumentException("스마트스토어 식별자(originProductNo)가 존재하지 않습니다.");
    }

    log.info("👉 [스마트스토어] 상품번호 {} 의 이미지/HTML 업데이트를 시작합니다.", originProductNo);

    try {
      // 🚀 1. 기존 상품 정보 전체 조회 (GET)
      // 보통 스마트스토어 API v2 기준 엔드포인트: /v2/products/{originProductNo}
      String productPath = "/v2/products/" + originProductNo;
      String existingJson = smartstoreRestClient.get(productPath);

      // 🚀 2. JSON 파싱 및 트리 객체(ObjectNode)로 변환 (수정을 위해)
      ObjectNode rootNode = (ObjectNode) objectMapper.readTree(existingJson);
      ObjectNode originProductNode = (ObjectNode) rootNode.path("originProduct");

      if (originProductNode.isMissingNode()) {
        throw new RuntimeException("응답에서 originProduct 노드를 찾을 수 없습니다.");
      }

      // 🚀 3. 상세 HTML 덮어쓰기
      originProductNode.put("detailContent", product.getDetailHtml());

      // 🚀 4. 이미지 덮어쓰기 (대표 이미지 + 추가 이미지)
      ObjectNode imagesNode = objectMapper.createObjectNode();

      // 4-1. 대표 이미지 (필수)
      ObjectNode repImageNode = objectMapper.createObjectNode();
      repImageNode.put("url", product.getRepImageUrl());
      imagesNode.set("representativeImage", repImageNode);

      // 4-2. 추가 이미지 (옵션) - Product에 hostedImages 같은 리스트 반환 Getter가 있다고 가정
      if (product.getHostedImages() != null && !product.getHostedImages().isEmpty()) {
        ArrayNode optionalImagesNode = objectMapper.createArrayNode();
        for (String imgUrl : product.getHostedImages()) {
          ObjectNode optImgNode = objectMapper.createObjectNode();
          optImgNode.put("url", imgUrl);
          optionalImagesNode.add(optImgNode);
        }
        imagesNode.set("optionalImages", optionalImagesNode);
      }

      // 갈아끼운 images 객체를 originProduct에 세팅
      originProductNode.set("images", imagesNode);

      // 🚀 5. 스마트스토어에 업데이트 요청 (PUT)
      // 스마트스토어 수정 API는 수정된 rootNode 전체를 그대로 밀어넣으면 됩니다.
      smartstoreRestClient.put(productPath, rootNode.toString());

      log.info("   ✅ [스마트스토어] 업데이트 완료! (상품번호: {})", originProductNo);

    } catch (Exception e) {
      log.error("   ❌ [스마트스토어] 업데이트 실패 (상품번호: {}): {}", originProductNo, e.getMessage());
      throw new RuntimeException("스마트스토어 상품 수정 중 오류가 발생했습니다.", e);
    }
  }

  private byte[] downloadImage(String imageUrl) {
    try {
      URL url = new URL(imageUrl);
      try (java.io.InputStream is = url.openStream()) {
        return is.readAllBytes();
      }
    } catch (Exception e) {
      log.error("   ❌ [스마트스토어] 이미지 다운로드 실패: {} - {}", imageUrl, e.getMessage());
      return null;
    }
  }

  private String ensureImageExtension(String url) {
    if (url == null || url.isBlank()) {
      return url;
    }
    // 확장자가 없으면 .jpg 추가 (네이버 검증 통과용)
    String lowUrl = url.toLowerCase();
    if (!lowUrl.contains(".jpg") && !lowUrl.contains(".jpeg") && !lowUrl.contains(".png") && !lowUrl.contains(".gif")) {
      return url + (url.contains("?") ? "&" : "?") + "f=.jpg";
    }
    return url;
  }
}
