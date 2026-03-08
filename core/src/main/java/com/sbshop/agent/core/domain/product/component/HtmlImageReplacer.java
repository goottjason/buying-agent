package com.sbshop.agent.core.domain.product.component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class HtmlImageReplacer {

  /**
   * 원본 HTML에서 특정 SKU가 포함된 모든 이미지 태그를 걷어내고, 새로운 이미지 태그들로 교체합니다.
   *
   * @param originalHtml 원본 상세 HTML
   * @param sku          타겟 상품의 SKU (예: "210909FM032")
   * @param hostedImages 새롭게 업로드된 클라우드플레어 이미지 URL 목록
   * @return 치환이 완료된 새 HTML
   */
  public String replaceImagesBySku(String originalHtml, String sku, List<String> hostedImages) {
    if (originalHtml == null || originalHtml.isEmpty()) {
      return originalHtml;
    }

    // 🚀 핵심 정규식: <img src="...SKU..."> 패턴과 그 뒤에 따라오는 <br> 태그들을 통째로 잡습니다.
    // (?i) : 대소문자 무시
    // <img[^>]*src=["'][^"']*SKU[^"']*["'][^>]*> : src 안에 SKU가 들어간 img 태그 매칭
    // (?:\s*<br\s*/?>\s*)* : img 태그 뒤에 연속해서 붙어있는 <br> 태그들을 모두 흡수
    String regex = "(?i)<img[^>]*src=[\"'][^\"']*" + Pattern.quote(sku) + "[^\"']*[\"'][^>]*>(?:\\s*<br\\s*/?>\\s*)*";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(originalHtml);

    StringBuffer sb = new StringBuffer();
    boolean isFirstMatch = true;

    while (matcher.find()) {
      if (isFirstMatch) {
        // 1. 첫 번째 매칭 위치에 "새로운 이미지 태그 묶음"을 와르르 쏟아 붓습니다.
        StringBuilder newTags = new StringBuilder();
        for (String newUrl : hostedImages) {
          newTags.append(String.format(
              "<img src=\"%s\" style=\"margin-left: auto; margin-right: auto; display: block;\"><br /><br /><br /><br />",
              newUrl
          ));
        }

        // Matcher.quoteReplacement는 URL 안에 특수문자($ 등)가 있을 때 에러나는 것을 방지합니다.
        matcher.appendReplacement(sb, Matcher.quoteReplacement(newTags.toString()));
        isFirstMatch = false;
      } else {
        // 2. 두 번째 매칭부터는 이미 새 이미지를 다 넣었으므로, 빈 문자열("")로 싹 다 지워버립니다. (초기화)
        matcher.appendReplacement(sb, "");
      }
    }
    matcher.appendTail(sb);

    return sb.toString();
  }
}