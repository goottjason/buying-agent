package com.sbshop.agent.infrastructure.client.sourcing;

import com.sbshop.agent.core.application.sourcing.client.ScraperClient;
import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import com.sbshop.agent.infrastructure.client.sourcing.parser.IherbProductParser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

/**
 * 아이허브 API 통신을 전담하는 구체 클라이언트
 * (JSON 통신만 담당하며, 데이터 해석은 Parser에게 위임합니다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IherbScraperClient implements ScraperClient {

  private final OkHttpClient httpClient = new OkHttpClient();
  private final IherbProductParser iherbProductParser; // 💡 파서를 주입받습니다.

  @Override
  public ScrapedProductDto scrape(String productUrl) {
    // 1. URL 추출
    String productId = extractProductId(productUrl);
    if (productId == null) {
      throw new IllegalArgumentException("올바른 아이허브 URL이 아닙니다: " + productUrl);
    }

    // 2. HTTP 통신 (순수 JSON 텍스트만 가져옴)
    String jsonResponse = fetchProductJson(productId);

    // 3. 파서에게 JSON 해석 및 도메인 변환 위임!
    return iherbProductParser.parse(productUrl, jsonResponse);
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
          // 랜덤 유저에이전트로 고도화 가능
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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