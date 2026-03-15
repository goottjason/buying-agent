package com.sbshop.agent.api.sourcing.dto.request;

import java.util.List;

/**
 * [프론트엔드 -> 백엔드]
 * 사용자가 입력한 소싱 URL 리스트를 담아오는 불변 요청 객체 (Record)
 */
public record ProductSourcingRequest(
    // 화면의 텍스트 상자에서 엔터로 구분된 URL들이 배열(List) 형태로 이곳에 담김.
    List<String> urls
) {}