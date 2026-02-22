package com.sbshop.agent.infrastructure.config; // 패키지명 확인 필수

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// QueryDSL을 사용하기 위한 엔진인 JPAQueryFactory를 스프링 빈으로 등록
@Configuration
public class QueryDslConfig {

  @PersistenceContext
  private EntityManager entityManager;

  @Bean
  public JPAQueryFactory jpaQueryFactory() {
    return new JPAQueryFactory(entityManager);
  }
}