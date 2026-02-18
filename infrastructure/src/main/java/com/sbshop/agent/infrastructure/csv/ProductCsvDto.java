package com.sbshop.agent.infrastructure.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class ProductCsvDto {

  // --- 기본 식별 정보 ---
  @CsvBindByName(column = "sbCode")
  private String sbCode;      // 우리 관리 코드
  @CsvBindByName(column = "korName")
  private String korName;     // 상품명(한글)
  @CsvBindByName(column = "engName")
  private String engName;     // 상품명(영문)
  @CsvBindByName(column = "ctgy")
  private String category;    // 카테고리 (아직 DB엔 매핑 안함)
  // --- 소싱 정보 ---
  @CsvBindByName(column = "link")
  private String sourceUrl;   // 구매처 링크
  @CsvBindByName(column = "costPrc")
  private String costPrice;   // 원가 (String -> BigDecimal 변환 필요)
  @CsvBindByName(column = "exch")
  private String exchangeRate; // 환율
  @CsvBindByName(column = "shippingPrc")
  private String shippingPrice; // 배송비
  @CsvBindByName(column = "mRate")
  private String marginRate;   // 마진율
  @CsvBindByName(column = "mktSalePrc")
  private String finalSalePrice; // 최종 판매가
  // --- 재고 및 물류 ---
  @CsvBindByName(column = "pendingStock")
  private String stockQuantity; // 재고
  @CsvBindByName(column = "weight")
  private String weight;        // 무게
  @CsvBindByName(column = "packInfo")
  private String packageInfo;   // 포장 정보
  @CsvBindByName(column = "packQty")
  private String packageQuantity; // 포장 수량 (DB에 컬럼 추가 고려)
  // --- 상세 ---
  @CsvBindByName(column = "html")
  private String htmlContent;   // 상세페이지 HTML
  @CsvBindByName(column = "memo")
  private String memo;          // 메모
  // --- 마켓별 ID (JSON으로 들어갈 녀석들) ---
  // [쿠팡]
  @CsvBindByName(column = "coupOptCode")
  private String coupOptCode;
  @CsvBindByName(column = "vendorItemId")
  private String vendorItemId;
  @CsvBindByName(column = "sellerProductId")
  private String sellerProductId;
  // [네이버 스마트스토어]
  @CsvBindByName(column = "navCode")
  private String navCode;
  // [지마켓/옥션 (ESM)]
  @CsvBindByName(column = "gmktCode")
  private String gmktCode;
  @CsvBindByName(column = "actCode")
  private String actCode;
  // [카페24 / 자사몰]
  @CsvBindByName(column = "cafeCode")
  private String cafeCode;
  @CsvBindByName(column = "cafeNo")
  private String cafeNo;
}