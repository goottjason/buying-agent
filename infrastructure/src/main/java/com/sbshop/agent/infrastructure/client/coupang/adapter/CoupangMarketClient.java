package com.sbshop.agent.infrastructure.client.coupang.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sbshop.agent.core.domain.market.client.MarketClient;
import com.sbshop.agent.core.domain.market.client.dto.MarketItemInfo;
import com.sbshop.agent.core.domain.market.model.enums.MarketType;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.infrastructure.client.coupang.client.CoupangRestClient;
import com.sbshop.agent.infrastructure.client.coupang.component.CoupangCategoryPredictor;
import com.sbshop.agent.infrastructure.client.coupang.component.CoupangMetaService;
import com.sbshop.agent.infrastructure.client.coupang.component.CoupangSearchTagGenerator;
import com.sbshop.agent.infrastructure.client.coupang.config.CoupangProperties;
import com.sbshop.agent.infrastructure.client.coupang.dto.CategoryMetaResult;
import com.sbshop.agent.infrastructure.client.coupang.dto.CoupangProductPayload;
import com.sbshop.agent.infrastructure.client.coupang.mapper.CoupangDataMapper;
import com.sbshop.agent.infrastructure.client.coupang.parser.CoupangProductParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupangMarketClient implements MarketClient {
  private final CoupangProperties properties;
  private final ObjectMapper objectMapper;
  private final CoupangRestClient restClient;

  private final CoupangCategoryPredictor categoryPredictor;
  private final CoupangProductParser productParser;
  private final CoupangSearchTagGenerator searchTagGenerator;
  private final CoupangDataMapper dataMapper;
  private final CoupangMetaService metaService;
  @Override
  public MarketType getSupportedMarket() {
    return MarketType.COUPANG;
  }

  @Override
  public Map<String, String> publish(Product product) {
    log.info("🚀 [쿠팡] 상품 등록 파이프라인 가동 - SKU: {}", product.getSku());

    try {
      // 1. 카테고리 번호 따오기 (화이트리스트 방어 적용)
      Long categoryId = categoryPredictor.predictCategory(product);

      // 2. 🚀 Redis에서 카테고리 메타 정보(필수 속성, 고시정보) 1초 만에 가져오기!
      CategoryMetaResult metaResult = metaService.getCategoryMeta(categoryId);

      // 3. (크롤링한 이미지나 태그가 있다면 이 시점에 가공...)
      List<String> tags = searchTagGenerator.generateTags(product);

      // 4. 🖼️ [이미지 처리] Usecase에서 이미 CDN에 올려둔 Hosted URL 매핑
      // (product 엔티티의 image 목록을 Coupang DTO 규격으로 변환)
      List<String> hostedUrls = product.getHostedImages();
      List<CoupangProductPayload.Item.Image> images = IntStream.range(0, hostedUrls.size())
          .mapToObj(i -> CoupangProductPayload.Item.Image.builder()
              .imageOrder(i)                                     // 인덱스(0, 1, 2...)를 그대로 순서로 사용
              .imageType(i == 0 ? "REPRESENTATION" : "DETAIL")   // 0번(첫 번째)만 대표 이미지, 나머지는 상세
              .vendorPath(hostedUrls.get(i))                     // String 자체(URL)를 vendorPath에 주입
              .build())
          .toList();

      // 4. 대망의 Payload 완벽 조립!! (앞서 만든 create 팩토리 메서드 호출)
      CoupangProductPayload payload = CoupangProductPayload.create(
          product,
          categoryId,
          product.getBaseName(), // masterName
          product.getBaseName(), // generalName
          product.getBrand(),
          product.getPriceInfo().getSalePrice().intValue(),
          tags,
          images,
          metaResult.notices(),     // 🚀 Redis에서 가져와 "상세참조"로 도배된 고시정보 주입!
          metaResult.attributes(),  // 🚀 Redis에서 가져온 필수 속성(1정, 상세참조) 주입!
          product.getDetailHtml()
      );

      // 5. 쿠팡 등록 API 찌르기!
      String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";
      String responseJson = restClient.requestWithBody("POST", path, payload);

      // [Step 5] 응답 파싱 및 식별자(ID) 추출
      JsonNode root = objectMapper.readTree(responseJson);
      if (root.path("data").isNull()) {
        throw new RuntimeException("쿠팡 등록 실패: " + root.path("message").asText());
      }

      String sellerProductId = root.path("data").asText();
      log.info("✅ [쿠팡] 상품 등록 성공! 상품 ID: {}", sellerProductId);

      Map<String, String> identifiers = new HashMap<>();
      identifiers.put("sellerProductId", sellerProductId);
      return identifiers;

    } catch (Exception e) {
      log.error("❌ [쿠팡] 연동 실패: {}", e.getMessage());
      throw new RuntimeException("쿠팡 연동 오류", e);
    }
  }

  /*private List<CoupangProductPayload.Attribute> fetchAndParseAttributes(Long categoryId, Product product) throws Exception {
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/meta/category-related-metas/display-category-codes/" + categoryId;
    String response = coupangApiClient.get(path); // 💡 GET 전용 메서드 호출!

    List<CoupangProductPayload.Attribute> attributes = new ArrayList<>();
    JsonNode attributesNode = objectMapper.readTree(response).path("data").path("attributes");

    for (JsonNode attr : attributesNode) {
      // 필수(MANDATORY) 속성만 골라냅니다.
      if ("MANDATORY".equals(attr.path("required").asText())) {
        String typeName = attr.path("attributeTypeName").asText();
        String groupNumber = attr.path("groupNumber").asText();

        // 🚀 핵심 1. 쿠팡이 내려준 exposed 값을 그대로 써야 '옵션 초과' 에러가 안 납니다!
        String exposed = attr.path("exposed").asText();

        String valueName = "";

        // 🚀 핵심 2. 레거시 로직 복원: groupNumber가 "NONE"이면 수량(개), 아니면 용량(단위)
        if ("NONE".equals(groupNumber)) {
          // 예: "1개", "3개"
          int qty = product.getLogisticsInfo() != null ? product.getLogisticsInfo().getBundleQuantity() : 1;
          valueName = qty + "개";
        } else {
          // DB에 저장된 용량과 단위를 사용 (예: 60 + 정 -> 60정)
          String capacity = product.getProductSpec() != null && product.getProductSpec().getCapacity() != null
              ? String.valueOf(product.getProductSpec().getCapacity().intValue()) : "1";

          // 주의: DB에 영문 ENUM(예: CAPSULE)으로 저장되어 있다면 한글(캡슐)로 변환해 주는 로직이 필요할 수 있습니다.
          // 임시로 DB에 있는 값을 그대로 붙이도록 짰습니다.
          String unit = product.getProductSpec() != null && product.getProductSpec().getMeasureUnit() != null
              ? product.getProductSpec().getMeasureUnit().name() : "개";

          valueName = capacity + unit;
        }

        attributes.add(new CoupangProductPayload.Attribute(typeName, valueName, exposed));
      }
    }
    return attributes;
  }*/


  public List<String> fetchAllMarketItemIds() {
    List<String> allIds = new ArrayList<>();
    String nextToken = ""; // 쿠팡은 페이지 번호 대신 이 토큰을 유지해야 함
    boolean hasMore = true;

    log.info("[쿠팡] 전체 상품 ID 싹쓸이 시작...");

    while (hasMore) {
      try {
        // 쿠팡 셀러 상품 목록 조회 API (maxPerPage는 100이 최대치)
        String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products"
            + "?vendorId=" + properties.getVendorId()
            + "&maxPerPage=100";

        // 다음 페이지 토큰이 있다면 쿼리스트링에 이어 붙임
        if (!nextToken.isEmpty()) {
          path += "&nextToken=" + nextToken;
        }

        // 인증, 헤더, 타임아웃 세팅이 모두 은닉됨
        String responseJson = restClient.get(path);

        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode dataNode = rootNode.path("data");

        if (dataNode.isEmpty()) {
          // dataNode가 비어있다면 이미 끝까지 조회한 것
          hasMore = false;
        } else {
          for (JsonNode node : dataNode) {
            // 쿠팡의 마스터 식별자인 sellerProductId 추출
            allIds.add(node.path("sellerProductId").asText());
          }

          // 응답으로 온 nextToken을 갱신
          nextToken = rootNode.path("nextToken").asText("");

          // 더 이상 nextToken이 갱신되지 않으면 이미 끝까지 조회한 것
          if (nextToken.isEmpty()) {
            hasMore = false;
          }

          Thread.sleep(300); // 쿠팡 API Rate Limit 방어
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
  public MarketItemInfo extractMarketItem(String marketItemId) { // sellerProductId

    // 상품 단건 상세 조회 (extractProductData 내부)
    String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketItemId
        + "?vendorId=" + properties.getVendorId(); // 🚀 여기도 추가!
    String responseJson = restClient.get(path);

    try {
      JsonNode dataNode = productParser.parseDataNode(responseJson);
      JsonNode firstItem = productParser.getFirstItem(dataNode);

      // TODO: 일괄 변경 전에는 cafe24Code(ex: P000BAAA000A)가 담겨 있음

      return MarketItemInfo.builder()
          // MarketRegistration 테이블에 업데이트
          .isMasterData(true)
          .name(firstItem.path("itemName").asText(null))
          .marketIdentifiers(dataMapper.buildIdentifiers(marketItemId, firstItem))
          .mappingKey(firstItem.path("externalVendorSku").asText(""))
          .brand(dataNode.path("brand").asText(null))
          .manufacturer(dataNode.path("manufacturer").asText(null))
          .barcode(firstItem.path("barcode").asText(null))
          .generalProductName(dataNode.path("generalProductName").asText(null))
          // DE에서 띄우는 그 노란색 경고는 자바의 제네릭(Generic) 타입 소거 특성 때문에 발생하는 'Unchecked assignment (확인되지 않은 할당)' 경고입니다. Map.class라고만 적으면 자바는 그 안에 <String, Object>가 들어갈지 확신할 수 없어서 찝찝해하는 것이죠.
          // @SuppressWarnings("unchecked")를 달아서 조용히 시킬 수도 있지만, Jackson 라이브러리가 제공하는 TypeReference를 사용하면 애초에 경고가 발생하지 않도록 훨씬 우아하게 해결할 수 있습니다!
          .rawData(dataMapper.buildRawData(dataNode))
          .build();
    } catch (Exception e) {
      log.error("❌ 쿠팡 상품 정보 추출 실패 (ID: {}): {}", marketItemId, e.getMessage());
      throw new RuntimeException("쿠팡 데이터 추출 오류", e);
    }
  }

  @Override
  public MarketItemInfo parseLocalData(Map<String, Object> rawData) {
    if (rawData == null || rawData.isEmpty()) {
      return MarketItemInfo.builder().build();
    }

    // =====================================================================
    // 1. 최상단 데이터 추출
    // =====================================================================
    // 쿠팡 JSON은 최상단에 상품명(displayProductName), 브랜드, 제조사 등이 있습니다.
    String displayProductName = rawData.get("displayProductName") != null ? String.valueOf(rawData.get("displayProductName")) : null;
    String brand = rawData.get("brand") != null ? String.valueOf(rawData.get("brand")) : null;

    // 💡 주의: 제공해주신 JSON에서는 키가 'manufacturer'가 아니라 'manufacture' 입니다!
    String manufacturer = rawData.get("manufacture") != null ? String.valueOf(rawData.get("manufacture")) : null;
    String generalProductName = rawData.get("generalProductName") != null ? String.valueOf(rawData.get("generalProductName")) : null;

    // =====================================================================
    // 2. 내부 items 배열(옵션 및 가격/재고) 추출
    // =====================================================================
    String externalVendorSku = "";
    String barcode = null;
    BigDecimal salePrice = null;
    Integer stock = 0;

    try {
      Object itemsObj = rawData.get("items");
      if (itemsObj instanceof java.util.List) {
        java.util.List<?> items = (java.util.List<?>) itemsObj;
        if (!items.isEmpty()) {
          @SuppressWarnings("unchecked")
          Map<String, Object> firstItem = (Map<String, Object>) items.get(0);

          // 매핑 키와 바코드
          externalVendorSku = firstItem.get("externalVendorSku") != null ? String.valueOf(firstItem.get("externalVendorSku")) : "";
          barcode = firstItem.get("barcode") != null ? String.valueOf(firstItem.get("barcode")) : null;

          // 🚀 가격과 재고 (쿠팡 JSON 키값: salePrice, maximumBuyCount)
          if (firstItem.get("salePrice") != null) {
            salePrice = new BigDecimal(String.valueOf(firstItem.get("salePrice")));
          }
          if (firstItem.get("maximumBuyCount") != null) {
            stock = Integer.parseInt(String.valueOf(firstItem.get("maximumBuyCount")));
          }
        }
      }
    } catch (Exception e) {
      log.warn("쿠팡 로컬 데이터 내부 items 배열 파싱 실패", e);
    }

    // =====================================================================
    // 3. 조립 및 반환
    // =====================================================================
    return MarketItemInfo.builder()
        .isMasterData(true)
        // 💡 팝업창 이름으로 items 안의 "2개"(itemName)보다는 최상단의 "줄리안베이커리..."(displayProductName)가 더 적합합니다.
        .name(displayProductName)
        .mappingKey(externalVendorSku)
        .brand(brand)
        .manufacturer(manufacturer)
        .barcode(barcode)
        .generalProductName(generalProductName)
        .salePrice(salePrice)
        .stock(stock)
        .rawData(rawData)
        .build();
  }

  @Override
  public Map<String, Object> syncPriceAndStock(String marketItemId, Map<String, Object> currentRawData, Integer price, Integer stock) {

    // 🚀 1. 실제 쿠팡 API를 찌르는 로직 (인프라 REST Client 호출)
    // coupangRestClient.updatePriceAndStock(marketItemId, price, stock);
    // log.info("쿠팡 API 가격/재고 업데이트 완료");

    // 🚀 2. API 전송이 성공했다면, 우리 로컬 Map 데이터를 패치(Patch)합니다.
    try {
      if (currentRawData != null && currentRawData.containsKey("items")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) currentRawData.get("items");

        if (items != null && !items.isEmpty()) {
          Map<String, Object> firstItem = items.get(0);

          // 값 덮어쓰기
          if (price != null) firstItem.put("salePrice", price);
          if (stock != null) firstItem.put("maximumBuyCount", stock);
        }
      }
    } catch (Exception e) {
      log.warn("쿠팡 로컬 Map 데이터 패치 중 오류 발생 (하지만 API 전송은 성공했을 수 있음)", e);
    }

    return currentRawData;
  }

  @Override
  public Map<String, Object> syncImagesAndHtml(String marketItemId, Map<String, Object> currentRawData, List<String> hostedImages, String newDetailHtml) {
    if (currentRawData == null || !currentRawData.containsKey("items")) return currentRawData;

    try {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) currentRawData.get("items");
      if (items != null && !items.isEmpty()) {
        Map<String, Object> firstItem = items.get(0);

        // =====================================================================
        // 1. 쿠팡 규격에 맞게 이미지 객체 리스트 조립
        // =====================================================================
        List<Map<String, Object>> coupangImages = new ArrayList<>();
        for (int i = 0; i < hostedImages.size(); i++) {
          Map<String, Object> imgMap = new HashMap<>();
          imgMap.put("imageOrder", i);
          // 첫 번째 이미지는 대표(REPRESENTATION), 나머지는 상세(DETAIL)
          imgMap.put("imageType", i == 0 ? "REPRESENTATION" : "DETAIL");
          imgMap.put("vendorPath", hostedImages.get(i));
          coupangImages.add(imgMap);
        }
        firstItem.put("images", coupangImages);

        // =====================================================================
        // 2. 쿠팡 규격에 맞게 HTML 상세 설명 조립
        // =====================================================================
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("contentsType", "HTML");
        contentMap.put("contentDetails", List.of(Map.of(
            "content", newDetailHtml,
            "detailType", "TEXT"
        )));
        contents.add(contentMap);
        firstItem.put("contents", contents);
      }

      // 🚀 3. API 통신 로직 (주석 해제 후 사용)
      // String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketItemId;
      String path = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";
      restClient.put(path, currentRawData);
      log.info("쿠팡 이미지/HTML 동기화 완료: {}", marketItemId);

    } catch (Exception e) {
      log.error("쿠팡 이미지/HTML 동기화 중 로컬 데이터 패치 실패", e);
    }

    return currentRawData; // 덮어써진 최신 맵 반환
  }

  public boolean deleteMarketProduct(String marketProductId) {
    log.warn("   👻 [쿠팡] 유령 상품(ID: {}) 발견! 삭제 대신 '전체 옵션 판매 중지' 로직을 가동합니다.", marketProductId);

    try {
      // 1. 상품 상세 정보를 조회하여 내부의 vendorItemId(옵션 ID) 목록을 가져옵니다.
      String detailPath = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductId
          + "?vendorId=" + properties.getVendorId();
      String responseJson = restClient.get(detailPath);

      JsonNode dataNode = productParser.parseDataNode(responseJson);
      JsonNode itemsNode = dataNode.path("items"); // 상품에 속한 옵션 배열

      if (!itemsNode.isArray() || itemsNode.isEmpty()) {
        log.warn("   ⚠️ [쿠팡] 판매 중지할 옵션(items)이 없습니다. (Market ID: {})", marketProductId);
        return false;
      }

      boolean allSuccess = true;

      // 2. 모든 옵션을 순회하며 개별 판매 중지(PUT) 요청을 보냅니다.
      for (JsonNode item : itemsNode) {
        String vendorItemId = item.path("vendorItemId").asText("");

        if (!vendorItemId.isEmpty()) {
          try {
            // 판매 중지 API 엔드포인트
            String stopPath = "/v2/providers/seller_api/apis/api/v1/marketplace/vendor-items/" + vendorItemId + "/sales/stop"
                + "?vendorId=" + properties.getVendorId();

            // 빈 바디 "{}" 와 함께 PUT 요청 발사!
            restClient.put(stopPath, "{}");
            log.info("      🛑 [쿠팡] 옵션(vendorItemId: {}) 판매 중지 성공", vendorItemId);

          } catch (Exception e) {
            log.error("      ❌ [쿠팡] 옵션(vendorItemId: {}) 판매 중지 실패: {}", vendorItemId, e.getMessage());
            allSuccess = false; // 하나라도 실패하면 false 마킹
          }
        }
      }

      // 🚀 [추가된 로직] 3. 모든 옵션 판매 중지가 완료되었다면, 본체(상품) 삭제(DELETE) 시도!
      if (allSuccess) {
        Thread.sleep(1000); // 쿠팡 API Rate Limit 방어
        log.info("   🛑 [쿠팡] 모든 옵션 판매 중지 완료. 이제 본체(상품) 삭제를 시도합니다.");
        try {
          String deletePath = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketProductId
              + "?vendorId=" + properties.getVendorId();

          restClient.delete(deletePath);
          log.info("   ✅ [쿠팡] 유령 상품 본체(ID: {}) 완벽 삭제(DELETE) 성공!", marketProductId);

        } catch (Exception e) {
          // 쿠팡 정책상 '판매 중지' 상태여도 삭제를 거부할 수 있습니다.
          // 하지만 이미 프론트에서는 상품이 내려갔으므로, 로컬 DB 정리를 위해 에러를 삼키고 성공 처리합니다.
          log.warn("   ⚠️ [쿠팡] 본체 삭제는 쿠팡 정책상 거부되었습니다 (판매 중지 상태 유지). " +
              "하지만 마켓에서 노출은 차단되었으므로 로컬 DB 찌꺼기를 삭제합니다.");
        }
        return true; // 매니저에게 "처리 완료됨"이라고 알려서 우리 로컬 DB 정보를 지우게 합니다!
      } else {
        return false;
      }

    } catch (Exception e) {
      log.error("   ❌ [쿠팡] 유령 상품 판매 중지 로직 수행 중 치명적 오류 발생 (ID: {}): {}", marketProductId, e.getMessage());
      return false;
    }
  }

  public void correctMarketSku(String marketItemId, String realSku) {
    try {
      log.info("   🛠️ [쿠팡] 가짜 SKU 교정 프로세스 시작 (ID: {} -> 목표 SKU: {})", marketItemId, realSku);

      // 1. 기존 상품 정보 전체 조회 (GET)
      String getPath = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products/" + marketItemId
          + "?vendorId=" + properties.getVendorId();
      String responseJson = restClient.get(getPath);

      // 2. JSON 파싱
      JsonNode rootNode = objectMapper.readTree(responseJson);
      JsonNode dataNode = rootNode.path("data");

      if (dataNode.isMissingNode() || !dataNode.has("items")) {
        log.warn("   ⚠️ [쿠팡] 상품 데이터나 옵션(items)이 없어 SKU 교정을 중단합니다. (ID: {})", marketItemId);
        return;
      }

      // 3. 옵션(items) 배열을 순회하며 externalVendorSku 값을 진짜 SKU로 변경!
      ArrayNode itemsNode = (ArrayNode) dataNode.path("items");
      boolean isModified = false;

      for (JsonNode item : itemsNode) {
        if (item.isObject()) {
          String currentSku = item.path("externalVendorSku").asText("");
          // 이미 진짜 SKU와 동일하다면 건너뜀 (불필요한 API 호출 방지)
          if (!currentSku.equals(realSku)) {
            ((ObjectNode) item).put("externalVendorSku", realSku);
            isModified = true;
          }
        }
      }

      // 변경할 게 없으면 그대로 종료
      if (!isModified) {
        log.info("   ✅ [쿠팡] 이미 진짜 SKU가 반영되어 있습니다. (ID: {})", marketItemId);
        return;
      }

      // 4. 수정한 data 덩어리를 문자열로 변환 (쿠팡 PUT 요청 바디 생성)
      String requestBody = objectMapper.writeValueAsString(dataNode);

      // 5. 수정된 데이터로 PUT 요청 발송 (덮어쓰기)
      String putPath = "/v2/providers/seller_api/apis/api/v1/marketplace/seller-products";
      // PUT 요청도 쿠팡 정책에 따라 vendorId를 요구할 수 있으니 안전하게 추가합니다.

      restClient.put(putPath, requestBody);

      log.info("   🎯 [쿠팡] 가짜 SKU 교정 완료! 서버 반영 성공 (ID: {}, 변경된 SKU: {})", marketItemId, realSku);

    } catch (Exception e) {
      log.error("   ❌ [쿠팡] 가짜 SKU 교정 실패 (ID: {}): {}", marketItemId, e.getMessage());
    }
  }

  public void updateProductImageAndHtml(Map<String, String> identifiers, Product product) {

  }
}
