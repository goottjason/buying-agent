package com.sbshop.agent.infrastructure.common; // 인프라 영역

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.domain.common.port.JsonUtilPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JacksonJsonAdapter implements JsonUtilPort {

  private final ObjectMapper objectMapper; // 여기서 당당하게 Jackson 사용!

  @Override
  public Map<String, Object> parseToMap(String jsonString) {
    try {
      return objectMapper.readValue(jsonString, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("JSON 파싱 실패", e);
    }
  }

  @Override
  public String toJsonString(Map<String, Object> map) {
    try {
      return objectMapper.writeValueAsString(map);
    } catch (Exception e) {
      throw new RuntimeException("JSON 직렬화 실패", e);
    }
  }
}