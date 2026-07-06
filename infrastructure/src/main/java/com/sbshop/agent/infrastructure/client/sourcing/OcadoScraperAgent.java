package com.sbshop.agent.infrastructure.client.sourcing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.sourcing.component.SourcingAgent;
import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;
import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcadoScraperAgent implements SourcingAgent {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper;

    @Override
    public SourcingSiteCode getSiteCode() {
        return SourcingSiteCode.OCADO;
    }

    @Override
    public ScrapedProductInfo scrapeProduct(String sourceUrl, String sourceProductCode) {
        String productId = sourceProductCode != null ? sourceProductCode : extractProductId(sourceUrl);
        if (productId == null) {
            throw new IllegalArgumentException("올바른 오카도(Ocado) URL이 아닙니다: " + sourceUrl);
        }

        // 역추적된 Ocado API 엔드포인트 호출
        String jsonResponse = fetchProductJson(productId);
        
        return parseResponse(productId, sourceUrl, jsonResponse);
    }

    @Override
    public List<ScrapedProductInfo> scrapeProducts(List<String> sourceUrls) {
        return sourceUrls.stream()
                .map(url -> scrapeProduct(url, null))
                .collect(Collectors.toList());
    }

    @Override
    public ScrapedProductInfo scrapePriceAndStock(String sourceUrl, String sourceProductCode) {
        return scrapeProduct(sourceUrl, sourceProductCode);
    }

    private String extractProductId(String url) {
        // 예: https://www.ocado.com/products/m-s-percy-pig-sweets-512345011
        Pattern pattern = Pattern.compile("-(\\d{8,9})/?$");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String fetchProductJson(String productId) {
        // Ocado의 내부 GraphQL/REST API (예시 역추적 엔드포인트)
        String apiUrl = "https://www.ocado.com/api/v1/products/" + productId;
        
        Request request = new Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .header("Accept-Language", "en-GB,en;q=0.9")
            .header("x-requested-with", "XMLHttpRequest") // Anti-bot 회피용 헤더
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else if (response.code() == 403 || response.code() == 429) {
                log.warn("Ocado 크롤링 차단 감지 (상태코드: {}). 브라우저 자동화(Playwright) 전환 필요.", response.code());
                // CRAWL_ERROR 상태 처리를 위해 익셉션 발생
                throw new RuntimeException("CRAWL_ERROR: Ocado 안티봇 캡챠에 의해 차단되었습니다.");
            } else {
                throw new RuntimeException("API 통신 에러: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Ocado 크롤링 실패", e);
        }
    }

    private ScrapedProductInfo parseResponse(String productId, String sourceUrl, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode productNode = root.path("product");
            
            String name = productNode.path("title").asText("");
            String brand = productNode.path("brand").asText("Ocado");
            String category = productNode.path("category").asText("식료품");
            
            BigDecimal price = BigDecimal.valueOf(productNode.path("price").path("current").asDouble(0.0));
            
            boolean inStock = productNode.path("availability").path("status").asText("").equalsIgnoreCase("IN_STOCK");
            
            String mainImage = productNode.path("image").path("main").asText("");
            List<String> addImages = new ArrayList<>();
            productNode.path("image").path("gallery").forEach(node -> addImages.add(node.asText()));

            return ScrapedProductInfo.builder()
                    .sourceProductCode(productId)
                    .sourceUrl(sourceUrl)
                    .nameEn(name)
                    .brand(brand)
                    .originalCategory(category)
                    .price(price)
                    .currency(SourcingSiteCode.OCADO.getDefaultCurrency()) // GBP
                    .stockStatus(inStock ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK)
                    .mainImageUrl(mainImage)
                    .additionalImageUrls(addImages)
                    .rawSourceHtml("<p>Mocked raw html for Ocado.</p>")
                    .scrapedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Ocado JSON 구조가 변경되었습니다. (DOM 변경 감지)", e);
        }
    }
}
