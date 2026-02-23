package com.sbshop.agent.infrastructure.product.csv.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductCsvDto {

  // --- 1. 기본 식별 및 스펙 정보 ---
  @CsvBindByName(column = "sku") private String sku;
  @CsvBindByName(column = "barcode") private String barcode;       // ★ 추가: 바코드
  @CsvBindByName(column = "brand") private String brand;
  @CsvBindByName(column = "name") private String name;
  @CsvBindByName(column = "originalName") private String originalName;
  @CsvBindByName(column = "capacity") private String capacity;
  @CsvBindByName(column = "measureUnit") private String measureUnit;
  @CsvBindByName(column = "category") private String category;

  // --- 2. 소싱 및 제조 정보 ---
  @CsvBindByName(column = "vendor") private String vendor;
  @CsvBindByName(column = "sourceUrl") private String sourceUrl;
  @CsvBindByName(column = "manufacturer") private String manufacturer;
  @CsvBindByName(column = "origin") private String origin;
  @CsvBindByName(column = "hsCode") private String hsCode;

  // --- 3. 가격 정보 (파싱 에러 방지를 위해 모두 String으로 받음) ---
  @CsvBindByName(column = "costPrice") private String costPrice;
  @CsvBindByName(column = "exchangeRate") private String exchangeRate;
  @CsvBindByName(column = "deliveryFee") private String deliveryFee;
  @CsvBindByName(column = "marginRate") private String marginRate;
  @CsvBindByName(column = "salePrice") private String salePrice;

  // --- 4. 물류 및 재고 정보 ---
  @CsvBindByName(column = "stock") private String stock;
  @CsvBindByName(column = "weight") private String weight;
  @CsvBindByName(column = "bundleQuantity") private String bundleQuantity;

  // --- 5. 상세 설명 및 부가 정보 ---
  @CsvBindByName(column = "searchKeywords") private String searchKeywords;
  @CsvBindByName(column = "detailHtml") private String detailHtml;
  @CsvBindByName(column = "memo") private String memo;

  // --- 6. 이미지 정보 (엑셀에서는 url1.jpg, url2.jpg 처럼 콤마로 연결해 입력) ---
  @CsvBindByName(column = "sourceImages") private String sourceImages;
  @CsvBindByName(column = "hostedImages") private String hostedImages;
}