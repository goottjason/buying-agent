package com.sbshop.agent.api.controller;

import com.sbshop.agent.api.service.ProductUploadService;
import com.sbshop.agent.infrastructure.csv.CsvParser;
import com.sbshop.agent.infrastructure.csv.ProductCsvDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

  private final CsvParser csvParser;
  private final ProductUploadService productUploadService;

  @PostMapping("/upload")
  public ResponseEntity<?> uploadCsv(@RequestParam("file") MultipartFile file) {
    log.info("CSV 업로드 요청: {}", file.getOriginalFilename());

    try {
      // 1. 파싱 (Infrastructure)
      List<ProductCsvDto> dtos = csvParser.parse(file);

      // 2. 저장 (Service)
      int count = productUploadService.saveProductsFromCsv(dtos);

      return ResponseEntity.ok(Map.of(
          "message", "성공적으로 업로드되었습니다.",
          "count", count
      ));

    } catch (Exception e) {
      log.error("업로드 실패", e);
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  // (테스트용) 상품 목록 조회 API도 하나 뚫어놓을까요?
  @GetMapping
  public ResponseEntity<?> getProducts() {
    // 일단 임시 메시지 리턴 (나중에 진짜 조회로 변경)
    return ResponseEntity.ok(Map.of("message", "상품 목록 조회 API 동작 중"));
  }
}