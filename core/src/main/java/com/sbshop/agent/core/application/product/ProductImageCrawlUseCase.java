package com.sbshop.agent.core.application.product;

import com.sbshop.agent.core.application.sourcing.client.ScraperClient;
import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import com.sbshop.agent.core.domain.product.component.ProductReader;
import com.sbshop.agent.core.domain.product.model.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 상품의 소싱 URL(아이허브)에서 이미지 URL 리스트만 추출하는 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageCrawlUseCase {

  private final ProductReader productReader;
  private final ScraperClient scraperClient;

  /**
   * 상품 ID로 DB에서 sourceUrl을 조회한 뒤, 아이허브를 크롤링하여 이미지 URL 리스트를 반환합니다.
   */
  public List<String> crawlImagesFromSource(Long productId) {
    Product product = productReader.read(productId);
    String sourceUrl = product.getSourcingInfo().getSourceUrl();

    if (sourceUrl == null || sourceUrl.isBlank()) {
      throw new IllegalStateException("해당 상품에 소싱 URL(sourceUrl)이 등록되어 있지 않습니다. (상품 ID: " + productId + ")");
    }

    log.info("아이허브 이미지 크롤링 시작 - 상품ID: {}, URL: {}", productId, sourceUrl);

    // 기존 ScraperClient를 재활용하여 크롤링 수행
    ScrapedProductDto scraped = scraperClient.scrape(sourceUrl);
    List<String> images = scraped.sourceImages();

    log.info("아이허브 이미지 크롤링 완료 - {}장 추출", images.size());
    return images;
  }
}
