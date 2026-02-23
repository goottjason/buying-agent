package com.sbshop.agent.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**") // 모든 API 주소에 대해
        .allowedOrigins("http://localhost:5174") // 리액트 프론트엔드 포트(5173) 접근 허용
        .allowedOrigins("http://localhost:5173") // 리액트 프론트엔드 포트(5173) 접근 허용
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}