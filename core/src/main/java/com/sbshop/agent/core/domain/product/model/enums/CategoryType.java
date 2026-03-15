package com.sbshop.agent.core.domain.product.model.enums;

import lombok.Getter;

@Getter
public enum CategoryType {

  // =====================================================================
  // 👑 1차 카테고리 (대분류 - 부모가 null)
  // =====================================================================
  SUPPLEMENT("건강식품/영양제", null),
  SPORTS("스포츠/피트니스", null),
  FOOD("일반식품", null),
  BEAUTY("뷰티/퍼스널케어", null),
  BABY("출산/유아동", null),
  PET("반려동물", null),
  LIVING("생활용품", null),
  UNKNOWN("미분류", null),

  // =====================================================================
  // 💊 2차 카테고리: 건강식품/영양제 (핵심 소싱 타겟)
  // =====================================================================
  VITAMIN("비타민", SUPPLEMENT),
  MINERAL("미네랄 (마그네슘/칼슘 등)", SUPPLEMENT),
  PROBIOTICS("유산균/프로바이오틱스", SUPPLEMENT),
  OMEGA3("오메가3/지방산", SUPPLEMENT),
  HERB("허브/식물추출물", SUPPLEMENT),
  SUPERFOOD("슈퍼푸드/버섯", SUPPLEMENT),
  SPECIAL_SUPPLEMENT("특수 영양제 (관절/눈/간 등)", SUPPLEMENT),

  // =====================================================================
  // 🏋️ 2차 카테고리: 스포츠/피트니스
  // =====================================================================
  PROTEIN("단백질/보충제", SPORTS),
  AMINO_ACID("아미노산/BCAA", SPORTS),
  PRE_WORKOUT("프리워크아웃/에너지", SPORTS),

  // =====================================================================
  // 🍯 2차 카테고리: 일반식품
  // =====================================================================
  COFFEE_TEA("커피/차", FOOD),
  SNACK("과자/단백질바", FOOD),
  HONEY_SWEETENER("꿀/감미료", FOOD),
  SPICE_SAUCE("향신료/소스", FOOD),

  // =====================================================================
  // 🧴 2차 카테고리: 뷰티/퍼스널케어
  // =====================================================================
  SKINCARE("스킨케어", BEAUTY),
  HAIR_BODY("헤어/바디케어", BEAUTY),
  ORAL_CARE("구강케어", BEAUTY);


  private final String title;
  private final CategoryType parent;

  CategoryType(String title, CategoryType parent) {
    this.title = title;
    this.parent = parent;
  }

  // =====================================================================
  // 🛠️ 도메인 유틸리티 메서드 (스마트 Enum)
  // =====================================================================

  /**
   * 최상위 부모 카테고리를 추적하여 반환합니다. (예: OMEGA3 -> SUPPLEMENT 반환)
   * HS Code 매핑이나 마켓 카테고리 매핑 시 매우 유용합니다.
   */
  public CategoryType getRootCategory() {
    if (this.parent == null) {
      return this;
    }
    return this.parent.getRootCategory();
  }

  /**
   * 해당 카테고리가 영양제(건강식품) 계열인지 확인합니다.
   * 통관 부호(HS Code)를 일괄 부여할 때 조건문으로 사용하기 좋습니다.
   */
  public boolean isSupplementFamily() {
    return this.getRootCategory() == SUPPLEMENT || this.getRootCategory() == SPORTS;
  }
}