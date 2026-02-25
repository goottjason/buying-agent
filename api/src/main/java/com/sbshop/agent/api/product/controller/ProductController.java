package com.sbshop.agent.api.product.controller;

import com.sbshop.agent.api.product.processor.ProductMigrationProcessor;
import com.sbshop.agent.api.product.dto.ProductBulkUpdateRequest;
import com.sbshop.agent.api.product.dto.ProductSaveRequest;
import com.sbshop.agent.api.product.dto.ProductSearchRequest;
import com.sbshop.agent.core.domain.product.component.ProductFinder;
import com.sbshop.agent.core.domain.product.component.ProductModifier;
import com.sbshop.agent.infrastructure.persistence.product.csv.CsvParser;
import com.sbshop.agent.infrastructure.persistence.product.csv.dto.ProductCsvDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController // 이 클래스가 REST API 요청을 처리하는 컨트롤러임을 명시합니다.
@RequestMapping("/api/products") // 이 컨트롤러의 모든 API 주소는 "/api/products"로 시작합니다.
@RequiredArgsConstructor // final이 붙은 필드(의존성)를 스프링이 알아서 채워주도록(주입) 합니다.
@Slf4j // 로그(log.info 등)를 찍기 위한 롬복 어노테이션입니다.
public class ProductController {

  // 우리가 만든 파서(엑셀 읽기 기계)와 서비스(DB 저장 기계)를 불러옵니다.
  private final CsvParser csvParser;
  private final ProductMigrationProcessor productMigrationProcessor;
  private final ProductFinder productFinder;
  private final ProductModifier productModifier;

  /**
   * 엑셀(CSV) 파일 업로드 API
   * POST http://localhost:8080/api/products/upload
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadCsv(@RequestParam("file") MultipartFile file) {
    // 1. 요청 확인 로그: 어떤 파일이 들어왔는지 파일명을 찍어봅니다.
    log.info("엑셀 파일 업로드 요청 수신: {}", file.getOriginalFilename());

    try {
      // 2. 파싱 (Infrastructure 영역)
      // 넘겨받은 파일을 CsvParser에게 주면, 알맹이만 쏙쏙 뽑아서 DTO 리스트로 돌려줍니다.
      List<ProductCsvDto> parsedDataList = csvParser.parse(file);
      log.info("엑셀 파싱 완료. 총 {}개의 데이터가 발견되었습니다.", parsedDataList.size());

      // 3. DB 저장 (Service 영역)
      // 파싱된 DTO 리스트를 Service에게 넘겨서 엔티티로 변환하고 DB에 저장하게 시킵니다.
      int successCount = productMigrationProcessor.migrateFromCsv(parsedDataList);
      log.info("DB 마이그레이션 완료. 성공: {}건", successCount);

      // 4. 성공 응답 (프론트엔드에게)
      // 성공적으로 끝났다는 메시지와 함께, 몇 개가 저장되었는지 숫자를 JSON 형태로 예쁘게 돌려줍니다.
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "상품 엑셀 업로드가 완료되었습니다.",
          "totalParsed", parsedDataList.size(),
          "successCount", successCount
      ));

    } catch (Exception e) {
      // 혹시라도 중간에 에러가 나면 서버가 뻗지 않게 잡아서 프론트엔드에 에러 메시지를 넘겨줍니다.
      log.error("엑셀 업로드 중 치명적인 오류 발생: {}", e.getMessage(), e);

      // HTTP 상태 코드 400(Bad Request)과 함께 에러 내용을 반환합니다.
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "message", "업로드 처리 중 오류가 발생했습니다.",
          "errorDetail", e.getMessage()
      ));
    }
  }

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

  // ★ 단건 등록 (POST)
  /*@PostMapping
  public ResponseEntity<?> createProduct(@RequestBody @Valid ProductSaveRequest request) {
    String createdSku = productModifier.createProduct(request.toCommand());
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "상품이 성공적으로 등록되었습니다.",
        "sku", createdSku
    ));
  }*/

  // ★ 단건 수정 (PUT)
  /*@PutMapping("/{sku}")
  public ResponseEntity<?> updateProduct(
      @PathVariable("sku") String sku,
      @RequestBody @Valid ProductSaveRequest request) {

    productModifier.updateProduct(sku, request.toCommand());

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "상품 정보가 수정되었습니다."
    ));
  }*/

  // ★ 엑셀 인라인 에디팅 일괄 저장 (PUT)
  /*@PutMapping("/bulk-edit")
  public ResponseEntity<?> bulkEditProducts(@RequestBody @Valid List<ProductSaveRequest> requests) {

    // request 리스트를 command 리스트로 변환하여 서비스로 넘김
    productModifier.bulkEditProducts(requests.stream()
        .map(ProductSaveRequest::toCommand)
        .toList());

    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", requests.size() + "개의 상품 정보가 성공적으로 수정되었습니다."
    ));
  }*/
}