package com.sbshop.agent.infrastructure.client.coupang.component;

import com.sbshop.agent.core.domain.product.model.Product;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CoupangSearchTagGenerator {

  // 💡 마케팅 치트키 (무조건 먹히는 직구 마법의 단어들)
  private static final List<String> MAGIC_KEYWORDS = List.of(
      "해외직구", "미국직구", "정품", "가성비", "영양제추천"
  );

  /**
   * 🎯 상품 정보를 바탕으로 쿠팡 SEO 최적화 태그(최대 20개)를 생성합니다.
   */
  public List<String> generateTags(Product product) {
    // 중복 방지를 위해 LinkedHashSet 사용 (순서 유지)
    Set<String> tags = new LinkedHashSet<>();

    // 1. [브랜드 파워] 브랜드명은 무조건 1순위 검색어!
    if (product.getBrand() != null) {
      tags.add(cleanText(product.getBrand()));
    }

    // 2. [핵심 키워드] 상품명에서 중요한 단어 추출 (예: "마그네슘", "타우레이트")
    // 보통 상품명을 공백으로 쪼개서 넣는 것이 가장 효과적입니다.
    if (product.getBaseName() != null) {
      String[] words = product.getBaseName().split("\\s+");
      for (String word : words) {
        String cleanedWord = cleanText(word);
        // 한 글자는 검색어로 의미가 없고, 쿠팡 제한(20자)을 넘는 것도 제외
        if (cleanedWord.length() > 1 && cleanedWord.length() <= 20) {
          tags.add(cleanedWord);
        }
      }
    }

    // 3. [마케팅 치트키] 직구 버프 단어들 추가
    tags.addAll(MAGIC_KEYWORDS);

    // 4. 쿠팡 규정에 맞게 최대 20개까지만 자르기
    return tags.stream()
        .limit(20)
        .collect(Collectors.toList());
  }

  /**
   * 🧹 쿠팡 태그 규정: 특수문자 금지 (영문, 숫자, 한글만 허용)
   */
  private String cleanText(String text) {
    return text.replaceAll("[^a-zA-Z0-9가-힣]", "");
  }
}