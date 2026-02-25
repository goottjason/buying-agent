package com.sbshop.agent.core.domain.product.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageInfo {

  // ★ 1. 소싱처 원본 이미지 URL 리스트 (0번이 대표 이미지)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "source_images", columnDefinition = "longtext")
  @Builder.Default
  private List<String> sourceImages = new ArrayList<>();

  // ★ 2. 우리 서버/ESM에 업로드 완료된 이미지 URL 리스트 (0번이 대표 이미지)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "hosted_images", columnDefinition = "longtext")
  @Builder.Default
  private List<String> hostedImages = new ArrayList<>();

}