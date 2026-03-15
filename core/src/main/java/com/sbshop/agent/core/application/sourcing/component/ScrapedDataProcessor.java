package com.sbshop.agent.core.application.sourcing.component;

import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 스크래퍼가 긁어온 날것의 데이터를 정제(Normalize)하고
 * 회사의 비즈니스 룰(Enrich)을 적용하는 전담 프로세서
 */
@Component
public class ScrapedDataProcessor {

  // 💡 비즈니스 정책: 환경변수에서 주입 (UseCase에서 이사 옴!)
  @Value("${app.sourcing.default-margin-rate:30}")
  private int defaultMarginRate;

  @Value("${app.sourcing.target-retail-price:60000}")
  private int targetRetailPrice;

  /**
   * 🚀 메인 가공 메서드: 정제와 보강을 한 번에 처리합니다.
   */
  public ScrapedProductDto process(ScrapedProductDto rawDto) {

    // 1. 데이터 정제 (Normalization)
    String cleanBrand = removeTrademarkSymbols(extractKoreanBrand(rawDto.brand()));
    String cleanBaseName = removeTrademarkSymbols(cleanBaseName(rawDto.baseName()));
    String cleanOriginalName = removeTrademarkSymbols(rawDto.originalName());

    // 2. 비즈니스 룰 적용 (Enrichment)
    int optimalBundle = calculateOptimalBundle(rawDto.costPrice(), defaultMarginRate, targetRetailPrice);

    // 3. 가공 완료된 완벽한 DTO 반환
    return rawDto.toBuilder()
        .brand(cleanBrand)
        .baseName(cleanBaseName)
        .originalName(cleanOriginalName) // 💡 원어 상품명도 정제된 데이터로 교체
        .bundleQuantity(optimalBundle) // 계산된 묶음수 주입
        .marginRate(defaultMarginRate) // 기본 마진율 주입
        .build();
  }

  // =====================================================================
  // 🛠️ 내부 비즈니스 & 정제 헬퍼 메서드들 (외부 접근 불가)
  // =====================================================================

  /**
   * 🚀 [신규 헬퍼] 마켓 업로드 시 에러를 유발하는 상표권 및 불필요한 특수문자 제거
   * (+, -, %, & 등 필수 기호는 안전하게 유지합니다)
   */
  private String removeTrademarkSymbols(String text) {
    if (text == null) return "";
    return text.replaceAll("[®™©‡†]", "").trim();
  }

  private int calculateOptimalBundle(int costPrice, int marginRate, int targetPrice) {
    if (costPrice <= 0) return 1;
    double pricePerItemWithMargin = costPrice * (1 + (marginRate / 100.0));
    int calculatedBundle = (int) Math.round(targetPrice / pricePerItemWithMargin);
    return Math.max(1, calculatedBundle);
  }

  /**
   * [정제 1] 브랜드명에서 한글만 쏙 뽑아오기
   */
  private String extractKoreanBrand(String brand) {
    if (brand == null || brand.isBlank()) return "";

    // 1. "Nordic Naturals (노르딕 내추럴스)" 형태 -> 괄호 안의 한글 추출
    Matcher matcher = Pattern.compile("\\((.*?[가-힣]+.*?)\\)").matcher(brand);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }

    // 2. 만약 괄호가 없고 "Nordic Naturals 노르딕 내추럴스" 형태라면 -> 한글 덩어리만 추출
    Matcher korMatcher = Pattern.compile("([가-힣\\s]+)").matcher(brand);
    if (korMatcher.find()) {
      String kor = korMatcher.group(1).trim();
      if (!kor.isBlank()) return kor;
    }

    return brand.trim(); // 한글이 아예 없으면 원본 반환
  }

  /**
   * [정제 2] 꼬리에 붙은 맛, 용량, 단위 텍스트 완벽 제거
   */
  private String cleanBaseName(String baseName) {
    if (baseName == null || baseName.isBlank()) return "";
    String cleaned = baseName;

    // 1. 끝에 괄호로 묶인 불필요한 정보 1차 제거 (예: "(100mg)")
    cleaned = cleaned.replaceAll("\\([^)]*\\)$", "").trim();

    // 🚀 2. 아이허브 특유의 꼬리표 + 맛 표현 완벽 타격 정규식!
    // [쉼표나공백] + [OO 맛] + [쉼표나공백] + [미니/베지 등 수식어] + [캡슐/소프트젤 등 제형] + [숫자] + [단위]
    String tailRegex = "(?:[,\\s]+)?(?:[가-힣a-zA-Z]+\\s*맛)?(?:[,\\s]+)?(?:미니|베지|식물성|액상)?\\s*(?:소프트젤|캡슐|타블렛|정|개|츄어블|구미|팩|병)?\\s*\\d+\\s*(?:소프트젤|캡슐|타블렛|정|개|츄어블|구미|팩|병|mg|g|ml|oz|IU|mcg)?$";

    cleaned = cleaned.replaceAll(tailRegex, "").trim();

    // 3. 만약 정제 후 맨 끝에 쉼표(,)가 남아있다면 깔끔하게 날려줌
    cleaned = cleaned.replaceAll(",$", "").trim();

    return cleaned;
  }
}