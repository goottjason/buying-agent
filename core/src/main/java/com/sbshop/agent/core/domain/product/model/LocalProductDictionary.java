package com.sbshop.agent.core.domain.product.model;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import java.util.*;

// 서버의 메모리에 올려두고 찾는 방식 (List는 처음부터 끝까지 훓어야 함, Map은 O(1))
public class LocalProductDictionary {

  // sku를 key로 모은 Product (Map)
  private final Map<String, Product> skuMap = new HashMap<>();

  // cafe24Code를 key로 모은 Product (Map)
  private final Map<String, Product> cafe24CodeMap = new HashMap<>();

  //
  private final Set<String> matchedSkus = new HashSet<>();

  public LocalProductDictionary(List<Product> products, List<MarketRegistration> cafe24Registrations) {
    // [1] skuMap
    if(products != null) {
      for (Product p : products) {
        if (p.getSku() != null) {
          skuMap.put(p.getSku(), p);
        }
      }
    }
    // [2] cafe24CodeMap
    if (cafe24Registrations != null) {
      for (MarketRegistration reg : cafe24Registrations) {
        String cafe24Code = reg.getMarketIdentifiers().get("product_code");
        if (cafe24Code != null) {
          cafe24CodeMap.put(cafe24Code, reg.getProduct());
        }
      }
    }
  }

  public Optional<Product> findAndMarkAsMatched(String mappingKey) {

    // skuMap에서 mappinKey가 있는지 조회
    Product product = skuMap.get(mappingKey);

    if (product == null) {
      // 쿠팡에 잘못 등록된 cafe24Code이므로 앞부분만 잘라내어 cafe24CodeMap에서 있는지 mappingKey가 있는지 조회
      String cafe24Code = mappingKey.substring(0, 8);
      product = cafe24CodeMap.get(cafe24Code);
    }


    if (product != null) {
      // 타겟 마켓에도 있고, Product에도 있으므로 sku를 모아둠 (추후에 Product에서 이 바구니에 없으면 마켓 서버에 재등록해야할 대상을 추릴 때 사용됨)
      matchedSkus.add(product.getSku());
    }
    return Optional.ofNullable(product);
  }

  // A - B (우리 DB에만 있고 마켓엔 없는 미등록 상품들 반환)
  public List<Product> getUnmatchedProducts() {
    return skuMap.values().stream()
        .distinct()
        .filter(p -> !matchedSkus.contains(p.getSku()))
        .toList();
  }
}