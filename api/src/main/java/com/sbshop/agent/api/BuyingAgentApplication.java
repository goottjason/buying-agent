package com.sbshop.agent.api;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync // ★ 이 어노테이션을 꼭 추가해주세요!
@SpringBootApplication
// 멀티모듈이라서 스캔 범위를 명시해주는 게 안전합니다.
@ComponentScan(basePackages = "com.sbshop.agent")
@EntityScan(basePackages = "com.sbshop.agent.core.domain")
@EnableJpaRepositories(basePackages = "com.sbshop.agent")
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO) // 🚀 이거 한 줄 추가! (안정적인 JSON 페이징 구조 보장)
// 스프링 부트 최신 버전에서는 페이징 객체(Page<>)를 JSON으로 바꿀 때, 구조가 불안정해질 수 있으니 명시적으로 설정을 켜달라고 요구합니다. 이 설정이 안 되어 있어서 리액트가 원하는 data.content 규격으로 JSON이 안 예쁘게 나갔을 확률이 높습니다.
public class BuyingAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(BuyingAgentApplication.class, args);
  }
}
