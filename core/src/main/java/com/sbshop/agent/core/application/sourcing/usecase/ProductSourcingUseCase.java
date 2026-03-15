package com.sbshop.agent.core.application.sourcing.usecase;

import com.sbshop.agent.core.application.sourcing.client.ScraperClient;
import com.sbshop.agent.core.application.sourcing.component.ScrapedDataProcessor; // 💡 통합 프로세서 도입
import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import java.util.ArrayList;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSourcingUseCase {

  private final ScraperClient scraperClient;
  private final ScrapedDataProcessor dataProcessor; // 🚀 정제+계산 전담 객체
  private final Random random = new Random();

  public List<ScrapedProductDto> sourceFromIherb(List<String> urls) {
    log.info("아이허브 크롤링 파이프라인 가동 - 총 {}건", urls.size());

    List<ScrapedProductDto> results = new ArrayList<>();

    for (int i = 0; i < urls.size(); i++) {
      String url = urls.get(i);
      log.info("[{}/{}] 크롤링 진행 중: {}", i + 1, urls.size(), url);

      // 1. 파서를 통해 순수 날것의 데이터 긁어오기
      ScrapedProductDto rawDto = scraperClient.scrape(url);

      // 🚀 2. 프로세서를 통해 데이터 정제 및 비즈니스 룰 일괄 적용!
      ScrapedProductDto processedDto = dataProcessor.process(rawDto);

      results.add(processedDto);

      // 3. 봇 차단 방지 딜레이
      if (i < urls.size() - 1) sleepRandomly();
    }

    return results;
  }

  private void sleepRandomly() {
    try {
      int delay = 500 + random.nextInt(500);
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}