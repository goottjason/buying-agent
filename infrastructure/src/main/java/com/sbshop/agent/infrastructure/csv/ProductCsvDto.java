package com.sbshop.agent.infrastructure.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductCsvDto {

  // 엑셀의 헤더명(column)과 똑같이 맞춰줍니다.
  @CsvBindByName(column = "sku") private String sku;
  @CsvBindByName(column = "name") private String name;
  @CsvBindByName(column = "originalName") private String originalName;
  @CsvBindByName(column = "category") private String category; // 예: COFFEE
  @CsvBindByName(column = "vendor") private String vendor;     // 예: COK
  @CsvBindByName(column = "sourceUrl") private String sourceUrl;

  // 숫자로 변환될 값들이지만, 파싱 에러 방지를 위해 일단 String으로 다 받습니다.
  @CsvBindByName(column = "costPrice") private String costPrice;
  @CsvBindByName(column = "exchangeRate") private String exchangeRate;
  @CsvBindByName(column = "deliveryFee") private String deliveryFee;
  @CsvBindByName(column = "marginRate") private String marginRate;
  @CsvBindByName(column = "salePrice") private String salePrice;

  @CsvBindByName(column = "stock") private String stock;
  @CsvBindByName(column = "weight") private String weight;
  @CsvBindByName(column = "bundleQuantity") private String bundleQuantity;

  @CsvBindByName(column = "detailHtml") private String detailHtml;
  @CsvBindByName(column = "memo") private String memo;
}