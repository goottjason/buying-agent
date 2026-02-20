package com.sbshop.agent.api.product;

import com.sbshop.agent.infrastructure.csv.CsvParser;
import com.sbshop.agent.infrastructure.csv.ProductCsvDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  /**
   * 엑셀(CSV) 파일 업로드 API
   * POST http://localhost:8080/api/products/upload
   */
  @PostMapping("/upload")
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
}