package com.sbshop.agent.infrastructure.external.coupang.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoupangProductParser {

  private final ObjectMapper objectMapper;

  /**
   * 응답 JSON에서 핵심 데이터인 "data" 노드만 추출합니다.
   */
  public JsonNode parseDataNode(String json) throws Exception {
    JsonNode rootNode = objectMapper.readTree(json);
    return rootNode.path("data");
  }

  /**
   * 쿠팡은 모든 핵심 정보(가격, SKU, 설명)가 옵션(items) 안에 있습니다.
   * 대표로 첫 번째 옵션을 안전하게 가져옵니다.
   */
  public JsonNode getFirstItem(JsonNode dataNode) {
    JsonNode items = dataNode.path("items");
    if (items.isArray() && !items.isEmpty()) {
      return items.get(0);
    }
    return objectMapper.createObjectNode(); // NPE 방지용 빈 노드
  }
}