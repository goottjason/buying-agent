package com.sbshop.agent.core.domain.product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductAppender {

  private final ProductRepository productRepository;

  // 순수하게 "상품을 저장한다"는 행위를 캡슐화합니다.
  public Product append(Product product) {
    // NOTE: 나중에 저장 전 유효성 검사 등의 도메인 로직이 추가된다면 여기에 작성합니다.
    return productRepository.save(product);
  }
}
