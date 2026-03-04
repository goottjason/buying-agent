package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.dto.ProductBulkUpdateRequest;
import com.sbshop.agent.api.product.dto.ProductSearchRequest;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.component.ProductModifier;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // 이 클래스가 REST API 요청을 처리하는 컨트롤러임을 명시합니다.
@RequestMapping("/api/products") // 이 컨트롤러의 모든 API 주소는 "/api/products"로 시작합니다.
@RequiredArgsConstructor // final이 붙은 필드(의존성)를 스프링이 알아서 채워주도록(주입) 합니다.
@Slf4j // 로그(log.info 등)를 찍기 위한 롬복 어노테이션입니다.
public class ProductController {

  private final ProductFinder productFinder;
  private final ProductModifier productModifier;



  /**
   * 상품 다건 조회 및 검색 API (페이징 & 동적 쿼리)
   * GET http://localhost:8080/api/products?keyword=비타민&page=0&size=50
   */
  @GetMapping
  public ResponseEntity<?> searchProducts(
      @ModelAttribute ProductSearchRequest request,
      @PageableDefault(size = 50) Pageable pageable) {

    return ResponseEntity.ok(productFinder.searchProducts(request.toCondition(), pageable));
  }

  /**
   * 다건 일괄 수정 API (PATCH)
   * 부분 수정이므로 PUT 대신 PATCH를 사용하는 것이 RESTful 설계의 정석입니다.
   */
  @PatchMapping("/bulk")
  public ResponseEntity<?> bulkUpdateProducts(@RequestBody @Valid ProductBulkUpdateRequest request) {
    // Request -> Command로 변환하여 Modifier에 전달
    int updatedCount = productModifier.bulkUpdate(request.toCommand());

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "일괄 수정이 완료되었습니다.",
        "updatedCount", updatedCount
    ));
  }

  /**
   * 다건 일괄 삭제 API (DELETE)
   * 프론트엔드에서 SKU 목록을 배열로 넘기면 소프트 삭제 처리합니다.
   */
  @DeleteMapping("/bulk")
  public ResponseEntity<?> bulkDeleteProducts(@RequestParam("skus") List<String> skus) {
    int deletedCount = productModifier.bulkDelete(skus);

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "선택한 상품이 안전하게 삭제(비활성화) 되었습니다.",
        "deletedCount", deletedCount
    ));
  }
}