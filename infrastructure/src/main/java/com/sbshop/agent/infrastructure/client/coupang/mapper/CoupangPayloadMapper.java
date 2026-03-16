package com.sbshop.agent.infrastructure.client.coupang.mapper;


import com.sbshop.agent.core.domain.product.model.Product;

import com.sbshop.agent.infrastructure.client.coupang.dto.CoupangProductPayload;
import java.util.ArrayList;
import java.util.List;

/**
 * 🚀 [역할] 순수 도메인 Product를 쿠팡 전용 Payload로 변환하는 조립 공장
 */
public class CoupangPayloadMapper {

  /**
   * 💡 외부(Client)에는 오직 이 메서드 하나만 노출합니다!
   */
  public static CoupangProductPayload toPayload(Product product, Long categoryId, List<CoupangProductPayload.Attribute> attributes) {
    CoupangProductPayload.Item item = CoupangProductPayload.Item.create(
        product,
        buildImages(product),
        buildNotices(product),
        attributes,
        buildContents(product)
    );
    return CoupangProductPayload.create(product, categoryId, List.of(item));
  }

  // =================================================================
  // 🛠️ 내부 조립 헬퍼 메서드들 (완벽하게 숨김 처리)
  // =================================================================

  private static List<CoupangProductPayload.Image> buildImages(Product product) {
    List<CoupangProductPayload.Image> result = new ArrayList<>();
    List<String> imageUrls = product.getImageInfo() != null ? product.getImageInfo().getHostedImages() : new ArrayList<>();
    if (imageUrls == null || imageUrls.isEmpty()) return result;

    for (int i = 0; i < imageUrls.size(); i++) {
      result.add(new CoupangProductPayload.Image(
          i, i == 0 ? "REPRESENTATION" : "DETAIL", imageUrls.get(i)
      ));
    }
    return result;
  }

  private static List<CoupangProductPayload.Content> buildContents(Product product) {
    CoupangProductPayload.ContentDetail detail = new CoupangProductPayload.ContentDetail(
        product.getDetailHtml() != null ? product.getDetailHtml() : "", "TEXT"
    );
    return List.of(new CoupangProductPayload.Content("TEXT", List.of(detail)));
  }

  private static List<CoupangProductPayload.Notice> buildNotices(Product product) {
    String category = product.getCategory() != null ? product.getCategory().getTitle() : "";
    if (category.contains("건강기능식품") || category.contains("비타민") || category.contains("영양제")) {
      return HEALTH_FUNCTIONAL_NOTICES;
    }
    return PROCESSED_FOOD_NOTICES;
  }

  // =================================================================
  // 📋 정적 상수: 쿠팡 필수 상품 고시 정보
  // =================================================================
  private static final List<CoupangProductPayload.Notice> HEALTH_FUNCTIONAL_NOTICES = List.of(
      new CoupangProductPayload.Notice("건강기능식품", "제품명", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "제조업소의 명칭과 소재지", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "소비기한 및 보관방법", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "포장단위별 내용물의 용량(중량), 수량", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "원료명 및 함량", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "영양정보", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "기능정보", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "섭취량, 섭취방법, 섭취 시 주의사항 및 부작용 발생 가능성", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "의약품 여부", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "유전자변형건강식품에 해당하는 경우의 표시", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "수입 건강기능식품 문구", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "소비자안전을 위한 주의사항", "상세페이지 참조"),
      new CoupangProductPayload.Notice("건강기능식품", "소비자상담관련 전화번호", "상세페이지 참조")
  );

  private static final List<CoupangProductPayload.Notice> PROCESSED_FOOD_NOTICES = List.of(
      new CoupangProductPayload.Notice("가공식품", "제품명", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "식품의 유형", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "생산자 및 소재지", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "제조연월일, 소비기한 또는 품질유지기한", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "포장단위별 내용물의 용량(중량), 수량", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "원재료명 및 함량", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "영양성분", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "유전자변형식품에 해당하는 경우의 표시", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "소비자안전을 위한 주의사항", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "수입식품 문구", "상세페이지 참조"),
      new CoupangProductPayload.Notice("가공식품", "소비자상담관련 전화번호", "상세페이지 참조")
  );

}