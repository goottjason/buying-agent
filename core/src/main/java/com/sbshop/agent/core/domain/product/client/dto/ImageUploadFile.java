package com.sbshop.agent.core.domain.product.client.dto;

import java.io.InputStream;

/**
 * 프레임워크(Spring Web)에 종속되지 않는 순수 도메인용 이미지 파일 객체
 */
public record ImageUploadFile(
    String originalFilename,
    String contentType,
    InputStream inputStream,
    long size
) {}