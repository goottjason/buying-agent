package com.sbshop.agent.core.domain.product.dto;

import java.io.InputStream;

/**
 * 프레임워크(MultipartFile)에 종속되지 않는 순수 자바 파일 전송 객체
 */
public record ImageUploadData(
    InputStream inputStream,
    String originalFilename,
    String contentType,
    long size
) {
}