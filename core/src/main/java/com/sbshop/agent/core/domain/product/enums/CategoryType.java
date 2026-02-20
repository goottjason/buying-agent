package com.sbshop.agent.core.domain.product.enums;

import lombok.Getter;

@Getter
public enum CategoryType {
  // 1차 카테고리 (부모가 null)
  FOOD("식품", null),
  LIVING("생활용품", null),

  // 2차 카테고리 (부모를 지정하여 계층화)
  COFFEE("커피/차", FOOD),
  SNACK("과자/간식", FOOD),
  DETERGENT("세제", LIVING);

  private final String title;
  private final CategoryType parent; // 상위 카테고리 참조

  CategoryType(String title, CategoryType parent) {
    this.title = title;
    this.parent = parent;
  }
}