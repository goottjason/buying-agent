package com.sbshop.agent.core.domain.common.port;

import java.util.Map;

public interface JsonUtilPort {
  Map<String, Object> parseToMap(String jsonString);
  String toJsonString(Map<String, Object> map);
}