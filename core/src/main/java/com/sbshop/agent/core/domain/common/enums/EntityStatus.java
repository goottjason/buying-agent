package com.sbshop.agent.core.domain.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EntityStatus {
  ACTIVE("활성"),
  INACTIVE("비활성"),
  DELETED("삭제");

  private final String label;

}
