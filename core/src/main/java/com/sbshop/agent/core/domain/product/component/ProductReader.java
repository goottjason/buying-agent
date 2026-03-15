package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.List;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductReader {

  private final ProductRepository repository;

  // 🚀 [신규] 검색어가 비어있으면 전체 조회, 있으면 검색 조회
  public Page<Product> searchProducts(String keyword, Pageable pageable) {

    // 🚀 [핵심 방어 로직] 정렬 조건이 비어있다면 강제로 'ID 내림차순(최신순)'을 주입합니다.
    Pageable sortedPageable = pageable.getSort().isSorted()
        ? pageable
        : PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "id") // 💡 무조건 최신 등록순!
        );

    if (!StringUtils.hasText(keyword)) {
      // 💡 일반 조회 시에도 최신순 적용
      return repository.findAll(sortedPageable);
    }

    // 💡 검색어 조회 시에도 최신순 적용
    return repository.searchByNameOrSku(keyword, sortedPageable);
  }

  public Product read(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
  }

  /**
   * 🚀 프리픽스(예: "20260315IHB")를 받아 다음으로 사용할 시퀀스 번호를 반환합니다.
   */
  public int getNextSkuSequence(String skuPrefix) {

    // 1. DB에서 오늘 날짜 + 해당 소싱처로 등록된 가장 마지막 SKU를 가져옵니다.
    String maxSku = repository.findMaxSkuByPrefix(skuPrefix);

    // 2. 만약 오늘 등록된 상품이 하나도 없다면, 당당하게 1번부터 시작!
    if (maxSku == null || maxSku.isBlank()) {
      return 1;
    }

    // 3. 가장 큰 SKU에서 프리픽스 길이를 제외한 뒷부분(숫자)만 잘라냅니다.
    // 예: "20260315IHB005" -> substring(11) -> "005"
    try {
      String sequenceStr = maxSku.substring(skuPrefix.length());
      int currentMaxSequence = Integer.parseInt(sequenceStr);

      // 4. 마지막 번호에 +1을 해서 반환합니다.
      return currentMaxSequence + 1;

    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      // 💡 누군가 DB에서 SKU 포맷을 임의로 망가뜨렸을 경우를 대비한 방어 로직
      log.warn("SKU 시퀀스 파싱 중 오류 발생. 비정상적인 SKU 포맷: {}", maxSku);

      // 예외가 터지면 일단 안전하게 999 같은 임시 번호를 던지거나 예외를 발생시킵니다.
      // 실무에서는 보통 예외를 던져서 저장을 막고 원인을 파악하게 합니다.
      throw new IllegalStateException("DB에 잘못된 포맷의 SKU가 존재합니다: " + maxSku, e);
    }
  }

  /*public List<Product> readAll() {
    return repository.findAll();
  }
  public Product read(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
  }

  public List<Product> readAllByIds(List<Long> ids) {
    return repository.findAllByIds(ids);
  }

  // 공통 예외 처리 로직을 품은 단건 조회
  public Product readBySku(String sku) {
    return repository.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("해당 SKU의 상품을 찾을 수 없습니다: " + sku));
  }

  // 일괄 작업을 위한 다건 조회
  public List<Product> readAllBySkus(List<String> skus) {
    List<Product> products = repository.findBySkuIn(skus);
    if (products.isEmpty()) {
      throw new IllegalArgumentException("요청한 SKU에 해당하는 상품이 하나도 없습니다.");
    }
    return products;
  }*/
}