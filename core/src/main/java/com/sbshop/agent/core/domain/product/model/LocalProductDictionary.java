package com.sbshop.agent.core.domain.product.model;

import com.sbshop.agent.core.domain.market.model.MarketRegistration;
import java.util.*;

// 서버의 메모리에 올려두고 찾는 방식 (List는 처음부터 끝까지 훓어야 함, Map은 O(1))
public class LocalProductDictionary {

  // 1. 진짜 SKU 전용 사전 (예: "250401IHB025" -> Product)
  private final Map<String, Product> skuMap = new HashMap<>();
  // 2. 🚀 카페24 우회(Fallback) 전용 사전 (예: "P000BAAA" -> Product)
  private final Map<String, Product> cafe24CodeMap = new HashMap<>();

  private final Set<String> matchedSkus = new HashSet<>(); // 교집합 기록용

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

  // 마켓 SKU로 우리 상품을 찾고, 찾으면 교집합(Matched)으로 기록!
  public Optional<Product> findAndMarkAsMatched(String marketSku) {
    if (marketSku == null || marketSku.isBlank()) return Optional.empty();

    // 1단계: 일단 진짜 SKU 사전에서 먼저 찾아본다. (스마트스토어 등 정상적인 경우)
    Product matched = skuMap.get(marketSku);

    // 2단계: 못 찾았는데, 쿠팡에서 온 SKU가 "P000BAAA000A" 형태라면?!
    if (matched == null && marketSku.startsWith("P") && marketSku.length() >= 8) {
      String cafe24Code = marketSku.substring(0, 8); // "P000BAAA" 만 톡 자름

      // 🚀 카페24 우회 사전에서 "P000BAAA"로 진짜 상품(250401IHB025)을 찾아냄!
      matched = cafe24CodeMap.get(cafe24Code);
    }

    if (matched != null) {
      matchedSkus.add(matched.getSku()); // 타겟 마켓에도 있고, Product에도 있는 상품
    }
    return Optional.ofNullable(matched);
  }

  // A - B (우리 DB에만 있고 마켓엔 없는 미등록 상품들 반환)
  public List<Product> getUnmatchedProducts() {
    return skuMap.values().stream()
        .distinct()
        .filter(p -> !matchedSkus.contains(p.getSku()))
        .toList();
  }
}