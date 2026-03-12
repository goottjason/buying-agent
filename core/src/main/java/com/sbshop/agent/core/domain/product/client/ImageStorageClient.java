package com.sbshop.agent.core.domain.product.client;

import com.sbshop.agent.core.domain.product.client.dto.ImageUploadFile;
import java.util.List;
import java.util.Map;

public interface ImageStorageClient {
  /**
   * 이미지들을 R2에 업로드하고, { "원본경로" : "클라우드플레어_퍼블릭_URL" } 맵을 반환합니다.
   */
  Map<String, String> uploadImages(List<ImageUploadFile> sourceImages);
}