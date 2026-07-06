package com.sbshop.agent.infrastructure.client.sourcing;

import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import com.sbshop.agent.core.domain.sourcing.component.SourcingAgent;
import com.sbshop.agent.core.domain.sourcing.dto.ScrapedProductInfo;
import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;
import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import com.sbshop.agent.infrastructure.client.sourcing.parser.IherbProductParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 아이허브 API 통신을 전담하는 구체 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IherbScraperClient implements SourcingAgent {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final IherbProductParser iherbProductParser;

    @Override
    public SourcingSiteCode getSiteCode() {
        return SourcingSiteCode.IHERB;
    }

    @Override
    public ScrapedProductInfo scrapeProduct(String sourceUrl, String sourceProductCode) {
        String productId = sourceProductCode != null ? sourceProductCode : extractProductId(sourceUrl);
        if (productId == null) {
            throw new IllegalArgumentException("올바른 아이허브 URL이 아닙니다: " + sourceUrl);
        }

        String jsonResponse = fetchProductJson(productId);
        
        // 기존 파서 사용 후 어댑팅 처리
        ScrapedProductDto oldDto = iherbProductParser.parse(sourceUrl, jsonResponse);

        List<String> images = oldDto.sourceImages();
        String mainImg = images != null && !images.isEmpty() ? images.get(0) : "";
        List<String> addImgs = images != null && images.size() > 1 ? images.subList(1, images.size()) : List.of();

        return ScrapedProductInfo.builder()
                .sourceProductCode(productId)
                .sourceUrl(sourceUrl)
                .nameEn(oldDto.originalName())
                .brand(oldDto.brand())
                .originalCategory(oldDto.rawCategory())
                .price(BigDecimal.valueOf(oldDto.costPrice()))
                .currency(SourcingSiteCode.IHERB.getDefaultCurrency())
                .stockStatus(oldDto.isAvailable() ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK)
                .mainImageUrl(mainImg)
                .additionalImageUrls(addImgs)
                .rawSourceHtml(oldDto.rawSourceHtml())
                .scrapedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<ScrapedProductInfo> scrapeProducts(List<String> sourceUrls) {
        // TODO: Rate Limit 등을 고려한 병렬 스크래핑 구현
        return sourceUrls.stream()
                .map(url -> scrapeProduct(url, null))
                .collect(Collectors.toList());
    }

    @Override
    public ScrapedProductInfo scrapePriceAndStock(String sourceUrl, String sourceProductCode) {
        // 아이허브는 단일 API 호출로 가격/재고가 한 번에 오므로 동일 로직 사용
        return scrapeProduct(sourceUrl, sourceProductCode);
    }

    private String extractProductId(String url) {
        Pattern pattern = Pattern.compile("/pr/[^/]+/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String fetchProductJson(String productId) {
        String apiUrl = "https://catalog.app.iherb.com/product/" + productId;
        int maxRetries = 3;

        for (int i = 0; i <= maxRetries; i++) {
            Request request = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else if (response.code() == 403) {
                    log.warn("아이허브 403 차단. 재시도 중... ({}/{})", i + 1, maxRetries);
                    Thread.sleep(2000L * (i + 1));
                } else if (response.code() == 404) {
                    throw new RuntimeException("상품을 찾을 수 없습니다 (404): " + productId);
                } else {
                    throw new RuntimeException("API 통신 에러: " + response.code());
                }
            } catch (Exception e) {
                if (i == maxRetries) throw new RuntimeException("아이허브 크롤링 실패", e);
            }
        }
        throw new RuntimeException("알 수 없는 에러 발생");
    }
}