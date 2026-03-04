package com.sbshop.agent.core.domain.product.port;

import com.sbshop.agent.core.domain.product.dto.ImageUploadData;

/**
 * Core 영역에서 바라보는 이미지 저장소 인터페이스
 * (AWS S3인지, R2인지, 로컬 디스크인지 Core는 알 필요가 없습니다.)
 */
public interface ImageStoragePort {
  String uploadImage(ImageUploadData fileData, String sku, int index);
}