package com.sbshop.agent.infrastructure.external.smartstore.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmartstoreProductParser {
  private final ObjectMapper objectMapper;

  /**
   * 응답 JSON을 파싱하여 Root 노드를 반환합니다.
   */
  public JsonNode parseRootNode(String json) throws Exception {
    return objectMapper.readTree(json);
  }

  /**
   * 스마트스토어는 핵심 데이터가 "originProduct" 노드 안에 모여있습니다.
   */
  public JsonNode getOriginProductNode(JsonNode rootNode) {
    return rootNode.path("originProduct");
  }

  /**
   * "originProduct" 안의 특정 텍스트 필드를 안전하게 꺼냅니다.
   */
  public String getTextFromOrigin(JsonNode rootNode, String fieldName) {
    return getOriginProductNode(rootNode).path(fieldName).asText("").trim();
  }
}
