package com.sbshop.agent.core.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VendorType {
  COK("코스트코 영국"),
  IHB("아이허브"),
  AMAZON_US("아마존 미국"),
  UNKNOWN("기타/미지정");

  private final String description;
}