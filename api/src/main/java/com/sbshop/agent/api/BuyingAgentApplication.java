package com.sbshop.agent.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
// 멀티모듈이라서 스캔 범위를 명시해주는 게 안전합니다.
@ComponentScan(basePackages = "com.sbshop.agent")
@EntityScan(basePackages = "com.sbshop.agent.core.domain")
@EnableJpaRepositories(basePackages = "com.sbshop.agent")
@EnableJpaAuditing
public class BuyingAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(BuyingAgentApplication.class, args);
  }
}
