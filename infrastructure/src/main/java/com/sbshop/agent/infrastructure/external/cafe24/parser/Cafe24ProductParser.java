package com.sbshop.agent.infrastructure.external.cafe24.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class Cafe24ProductParser {

  private final ObjectMapper objectMapper;

  /**
   * API 응답 전체에서 "product" 노드만 안전하게 추출합니다.
   */
  public JsonNode parseProductNode(String json) throws IOException {
    JsonNode rootNode = objectMapper.readTree(json);
    return rootNode.path("product"); // 없는 경우 MissingNode 반환 (NPE 방지)
  }

  /**
   * 특정 노드에서 텍스트 값을 안전하게 가져옵니다.
   */
  public String getText(JsonNode node, String fieldName) {
    return node.path(fieldName).asText("").trim();
  }

  /**
   * 변종(variants) 목록 노드를 가져옵니다.
   */
  public JsonNode getVariantsNode(JsonNode productNode) {
    return productNode.path("variants");
  }
}