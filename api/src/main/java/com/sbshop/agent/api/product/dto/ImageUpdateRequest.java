package com.sbshop.agent.api.product.dto;

import java.util.List;

// 업로드할 원본 이미지 경로 또는 URL 목록
public record ImageUpdateRequest(
    List<String> sourceImages
) {}