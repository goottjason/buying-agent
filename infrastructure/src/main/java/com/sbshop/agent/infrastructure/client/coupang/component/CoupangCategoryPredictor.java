package com.sbshop.agent.infrastructure.client.coupang.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.infrastructure.client.coupang.client.CoupangRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service // 🚀 스프링 빈으로 등록
@RequiredArgsConstructor // 🚀 final 필드 자동 의존성 주입
public class CoupangCategoryPredictor {

  private final CoupangRestClient restClient;   // (종원님이 구현하신 HTTP 클라이언트 객체 이름에 맞게 수정하세요)
  private final ObjectMapper objectMapper;

  // =========================================================================
  // 🛡️ [화이트리스트] 안전한 해외구매대행 카테고리 모음 (건기식 + 가공식품)
  // =========================================================================
  private static final Set<Long> SAFE_OVERSEAS_CATEGORIES = Set.of(
      // 💊 1. 건강기능식품 (핵심 매출처)
      73132L, // 종합비타민
      73133L, // 단일비타민 (비타민C, D 등)
      73134L, // 미네랄 (마그네슘, 칼슘, 아연 등)
      73138L, // 오메가3/크릴오일
      73141L, // 프로바이오틱스/유산균
      73142L, // 밀크씨슬/간건강
      73144L, // 루테인/눈건강
      73145L, // 헬스/단백질 보충제
      73146L, // 콜라겐/이너뷰티
      73199L, // 기타 건강보조식품

      // 🍪 2. 가공식품 (아이허브 간식/차/커피 류)
      73859L, // 초콜릿
      73861L, // 캔디/젤리
      73872L, // 스낵/과자류
      73905L, // 시리얼/그래놀라
      73946L, // 잼/시럽
      74154L, // 허브차/전통차
      74187L  // 원두/드립커피
  );

  // 🚀 [핵심 2] 예측 실패 시 도망갈 최후의 보루 (폴백 카테고리)
  // 쿠팡이 이상한 화장품이나 공구 카테고리를 추천하면 무조건 이리로 빠집니다.
  private static final Long FALLBACK_HEALTH_CATEGORY_ID = 73199L; // 기타 건강보조식품

  public Long predictCategory(Product product) throws Exception {
    String path = "/v2/providers/openapi/apis/api/v1/categorization/predict";
    Map<String, String> body = Map.of(
        "productName", product.getBaseName(),
        "brand", product.getBrand()
    );

    try {
      // 1. 쿠팡 예측 API 호출
      String response = restClient.requestWithBody("POST", path, body); // 메서드명은 실제 환경에 맞게 조정
      Long predictedId = objectMapper.readTree(response).path("data").path("predictedCategoryId").asLong();

      // 2. 🚀 화이트리스트 검증: 추천받은 카테고리가 안전망 안에 있는지 확인!
      if (SAFE_OVERSEAS_CATEGORIES.contains(predictedId)) {
        log.info("[Category Predict] 안전 카테고리 매칭 성공: 상품명={}, 예측ID={}", product.getBaseName(), predictedId);
        return predictedId;
      }

      // 3. 엉뚱한 카테고리를 추천했다면 가차 없이 폴백 카테고리로 꽂아버림
      log.warn("[Category Predict] ⚠️ 위험 카테고리 감지! 폴백으로 우회: 상품명={}, 예측ID={}", product.getBaseName(), predictedId);
      return FALLBACK_HEALTH_CATEGORY_ID;

    } catch (Exception e) {
      // API 호출 자체에 실패했을 때도 등록을 포기하지 않고 폴백으로 방어!
      log.error("[Category Predict] ❌ API 호출 실패. 폴백 카테고리 반환. 원인: {}", e.getMessage());
      return FALLBACK_HEALTH_CATEGORY_ID;
    }
  }
}