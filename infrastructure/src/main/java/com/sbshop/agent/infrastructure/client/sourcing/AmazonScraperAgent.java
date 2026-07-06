package com.sbshop.agent.infrastructure.client.sourcing;

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
public class AmazonScraperAgent implements SourcingAgent {

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public SourcingSiteCode getSiteCode() {
        return SourcingSiteCode.AMAZON_US; // US/UK 동적으로 처리할 필요 있음. 현재는 US로 기본 반환
    }

    @Override
    public ScrapedProductInfo scrapeProduct(String sourceUrl, String sourceProductCode) {
        String asin = sourceProductCode != null ? sourceProductCode : extractAsin(sourceUrl);
        if (asin == null) {
            throw new IllegalArgumentException("올바른 Amazon URL이 아닙니다: " + sourceUrl);
        }

        // Amazon은 API 역추적이 불가능하므로, 보통 HTML 파싱이나 API 서비스를 사용함
        // 여기서는 CRAWL_ERROR 구조를 방어하기 위한 방어적 HTML 파싱 시도 로직
        String htmlResponse = fetchProductHtml(sourceUrl);
        return parseHtmlResponse(asin, sourceUrl, htmlResponse);
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

    private String extractAsin(String url) {
        Pattern pattern = Pattern.compile("/dp/([A-Z0-9]{10})");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String fetchProductHtml(String url) {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String bodyString = response.body().string();
                if (bodyString.contains("api-services-support@amazon.com") || bodyString.contains("Type the characters you see in this image")) {
                    throw new RuntimeException("CRAWL_ERROR: Amazon 캡챠(CAPTCHA) 페이지에 의해 차단되었습니다.");
                }
                return bodyString;
            } else {
                throw new RuntimeException("API 통신 에러: " + response.code());
            }
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Amazon 크롤링 실패", e);
        }
    }

    private ScrapedProductInfo parseHtmlResponse(String asin, String sourceUrl, String html) {
        // 실제로는 Jsoup 등으로 파싱해야 하나, 구조만 잡습니다.
        try {
            boolean isCaptcha = html.contains("CAPTCHA");
            if(isCaptcha) throw new RuntimeException("CRAWL_ERROR: 캡챠 발생");

            return ScrapedProductInfo.builder()
                    .sourceProductCode(asin)
                    .sourceUrl(sourceUrl)
                    .nameEn("Amazon Mock Product")
                    .brand("Amazon")
                    .originalCategory("General")
                    .price(BigDecimal.valueOf(19.99))
                    .currency(sourceUrl.contains(".co.uk") ? SourcingSiteCode.AMAZON_UK.getDefaultCurrency() : SourcingSiteCode.AMAZON_US.getDefaultCurrency())
                    .stockStatus(StockStatus.IN_STOCK)
                    .mainImageUrl("https://amazon.com/mock.jpg")
                    .additionalImageUrls(new ArrayList<>())
                    .rawSourceHtml("<div>Mocked</div>")
                    .scrapedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("CRAWL_ERROR: Amazon DOM 파싱 실패", e);
        }
    }
}
