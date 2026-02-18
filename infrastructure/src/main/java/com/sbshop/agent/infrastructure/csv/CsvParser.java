package com.sbshop.agent.infrastructure.csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvParser {

  public List<ProductCsvDto> parse(MultipartFile file) {
    // 한글 깨짐 방지를 위해 UTF-8 인코딩 명시
    try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      CsvToBean<ProductCsvDto> csvToBean = new CsvToBeanBuilder<ProductCsvDto>(reader)
          .withType(ProductCsvDto.class)
          .withIgnoreLeadingWhiteSpace(true) // 앞쪽 공백 무시
          .withIgnoreEmptyLine(true)         // 빈 줄 무시
          .withType(ProductCsvDto.class)
          .build();

      return csvToBean.parse();
    } catch (Exception e) {
      throw new RuntimeException("CSV 파일 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }
}