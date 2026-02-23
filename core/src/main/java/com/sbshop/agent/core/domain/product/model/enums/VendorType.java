package com.sbshop.agent.core.domain.product.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VendorType {
  COK("코스트코 영국"),
  IHB("아이허브"),
  AMS("아마존 미국"),
  FTN("포트넘앤메이슨"),
  OCD("오카도"),
  TES("테스코"),
  VTB("비타바이오틱스"),
  UNKNOWN("기타/미지정");

  private final String description;
}