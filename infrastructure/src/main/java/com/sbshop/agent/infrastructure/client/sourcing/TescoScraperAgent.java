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
public class TescoScraperAgent implements SourcingAgent {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper;

    @Override
    public SourcingSiteCode getSiteCode() {
        return SourcingSiteCode.TESCO;
    }

    @Override
    public ScrapedProductInfo scrapeProduct(String sourceUrl, String sourceProductCode) {
        String productId = sourceProductCode != null ? sourceProductCode : extractProductId(sourceUrl);
        if (productId == null) {
            throw new IllegalArgumentException("올바른 테스코(Tesco) URL이 아닙니다: " + sourceUrl);
        }

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
        // 예: https://www.tesco.com/groceries/en-GB/products/254921648
        Pattern pattern = Pattern.compile("/products/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String fetchProductJson(String productId) {
        // 역추적된 테스코 GraphQL 엔드포인트 시뮬레이션
        String apiUrl = "https://www.tesco.com/groceries/en-GB/resources/products/" + productId;
        
        Request request = new Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else if (response.code() == 403 || response.code() == 429) {
                log.warn("Tesco 크롤링 차단 감지 (상태코드: {}).", response.code());
                throw new RuntimeException("CRAWL_ERROR: Tesco 안티봇 서버 차단 감지.");
            } else {
                throw new RuntimeException("API 통신 에러: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Tesco 크롤링 실패", e);
        }
    }

    private ScrapedProductInfo parseResponse(String productId, String sourceUrl, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode productInfo = root.path("product");
            
            String name = productInfo.path("title").asText("");
            String brand = productInfo.path("brand").path("name").asText("Tesco");
            
            BigDecimal price = BigDecimal.valueOf(productInfo.path("price").asDouble(0.0));
            boolean inStock = productInfo.path("status").asText("").equalsIgnoreCase("Available");
            
            String mainImage = productInfo.path("defaultImageUrl").asText("");

            return ScrapedProductInfo.builder()
                    .sourceProductCode(productId)
                    .sourceUrl(sourceUrl)
                    .nameEn(name)
                    .brand(brand)
                    .originalCategory("식료품") // 임시 할당
                    .price(price)
                    .currency(SourcingSiteCode.TESCO.getDefaultCurrency())
                    .stockStatus(inStock ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK)
                    .mainImageUrl(mainImage)
                    .additionalImageUrls(new ArrayList<>())
                    .rawSourceHtml("<div>Mocked HTML for Tesco</div>")
                    .scrapedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Tesco JSON 구조가 변경되었습니다.", e);
        }
    }
}
