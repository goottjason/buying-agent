package com.sbshop.agent.infrastructure.product.csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.sbshop.agent.infrastructure.product.csv.dto.ProductCsvDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvParser {

  /**
   * 프론트엔드에서 올라온 CSV 파일을 받아서 ProductCsvDto 리스트로 변환합니다.
   */
  public List<ProductCsvDto> parse(MultipartFile file) {
    // 1. Reader 준비: 엑셀(CSV) 파일에 한글이 있을 수 있으니 UTF-8 인코딩으로 읽겠다고 명시합니다.
    try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

      // ★ 추가된 마법의 코드: 파일 맨 앞의 BOM(유령문자)을 확인하고 건너뜁니다.
      reader.mark(1);
      if (reader.read() != 0xFEFF) {
        reader.reset(); // 유령문자가 아니면 다시 원래 자리로 되돌림
      }

      // 2. OpenCSV의 마법사(Builder)를 부릅니다.
      CsvToBean<ProductCsvDto> csvToBean = new CsvToBeanBuilder<ProductCsvDto>(reader)
          .withType(ProductCsvDto.class) // 어떤 DTO 클래스에 담을지 알려줍니다.
          .withIgnoreLeadingWhiteSpace(true) // 데이터 앞쪽에 실수로 들어간 띄어쓰기를 무시합니다.
          .withIgnoreEmptyLine(true)         // 빈 줄이 있으면 에러 내지 말고 그냥 건너뜁니다.
          .build();

      // 3. 엑셀의 모든 줄을 읽어서 DTO 리스트로 쫙 뽑아냅니다.
      return csvToBean.parse();

    } catch (Exception e) {
      // 파일을 읽다가 문제가 생기면 예외를 던집니다.
      throw new RuntimeException("CSV 파일 파싱 중 오류가 발생했습니다. 양식을 확인해주세요.", e);
    }
  }
}