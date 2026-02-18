package com.sbshop.agent.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**") // 모든 API 경로에 대해
        .allowedOrigins("http://localhost:5173") // 리액트 서버 주소 허용
        .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 HTTP 메서드
        .allowCredentials(true);
  }
}