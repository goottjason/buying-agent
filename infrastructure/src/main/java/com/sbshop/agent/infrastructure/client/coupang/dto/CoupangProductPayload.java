package com.sbshop.agent.infrastructure.client.coupang.dto;

import com.sbshop.agent.core.domain.product.model.Product;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🚀 외부 쿠팡 API로 전송할 JSON Payload (불변 객체)
 * - 구매대행(건기식/식품) 도메인에 최적화된 마스터 정책 포함
 */
@Builder
public record CoupangProductPayload(
    Long displayCategoryCode,               // [파라미터] 노출카테고리코드: 자동매칭 방지용 예측 코드
    String sellerProductName,               // [파라미터] 등록상품명: 마스터 상품명 (ex. KAL 마그네슘 타우레이트 400mg 90정 2개)
    String vendorId,                        // [기본값] 판매자ID: "A00213055"
    String saleStartedAt,                   // [기본값] 판매시작일시: API 호출 시점 (yyyy-MM-dd'T'HH:mm:ss)
    String saleEndedAt,                     // [기본값] 판매종료일시: "2099-12-31T23:59:59"
    String displayProductName,              // [파라미터] 노출상품명: 마스터 상품명과 동일하게 주입
    String brand,                           // [파라미터] 브랜드명: 공백/특수문자 제거된 영문 브랜드
    String generalProductName,              // [파라미터] 제품명: 제품 본연의 이름 + 함량 (ex. 마그네슘 타우레이트 400mg)
    String deliveryMethod,                  // [기본값] 배송방법: "AGENT_BUY" (구매대행 필수)
    String deliveryCompanyCode,             // [기본값] 택배사코드: "CJGLS"
    String deliveryChargeType,              // [기본값] 배송비종류: "FREE" (무료배송 전략)
    Integer deliveryCharge,                 // [기본값] 기본배송비: 0
    Integer freeShipOverAmount,             // [기본값] 조건부무료배송기준금액: 0
    Integer deliveryChargeOnReturn,         // [기본값] 초도반품배송비: 15000 (해외 물류비 반영)
    String remoteAreaDeliverable,           // [기본값] 도서산간배송여부: "N" (해외배송 리스크 차단)
    String unionDeliveryType,               // [기본값] 묶음배송여부: "UNION_DELIVERY" (객단가 향상)
    String returnCenterCode,                // [기본값] 반품지센터코드: "1000519746" (반드시 국내 반품지 코드)
    String returnChargeName,                // [기본값] 반품지명: "서울 금천구"
    String companyContactNumber,            // [기본값] 반품지연락처: "010-2597-2480"
    String returnZipCode,                   // [기본값] 반품지우편번호: "08529"
    String returnAddress,                   // [기본값] 반품지기본주소: "서울특별시 금천구 시흥대로153길 90-4"
    String returnAddressDetail,             // [기본값] 반품지상세주소: "103호"
    Integer returnCharge,                   // [기본값] 반품회수편도배송비: 15000 (초도반품비와 1:1 매칭)
    Integer outboundShippingPlaceCode,      // [기본값] 출고지주소코드: 1206157 (반드시 해외 출고지 코드)
    String vendorUserId,                    // [기본값] 실사용자로그인ID: "shouldbeshop"
    Boolean requested,                      // [기본값] 자동승인요청여부: true
    List<Item> items                        // [파라미터] 조립된 단일 옵션 목록 (건기식 묶음 상품)
) {

  @Builder
  public record Item(
      String itemName,                      // [파라미터] 업체상품옵션명: (ex. "90정 2개")
      Integer originalPrice,                // [계산값] 정가: 판매가의 133% 세팅 (할인율 노출용)
      Integer salePrice,                    // [파라미터] 최종판매가
      Integer maximumBuyCount,              // [기본값] 판매가능수량: 999 (넉넉한 가상 재고)
      Integer maximumBuyForPerson,          // [계산값] 인당최대구매수량: 6 ÷ 묶음수량 (건기식 통관 방어 공식)
      Integer maximumBuyForPersonPeriod,    // [기본값] 인당최대구매수량제한기간: 1 (입항일 리셋)
      Integer outboundShippingTimeDay,      // [기본값] 기준출고일: 3 (여유있는 출고일)
      Integer unitCount,                    // [기본값] 단위수량: 0 (안전제일 비노출)
      String adultOnly,                     // [기본값] 성인여부: "EVERYONE"
      String taxType,                       // [기본값] 과세여부: "TAX"
      String parallelImported,              // [기본값] 병행수입여부: "NOT_PARALLEL_IMPORTED" (서류면제)
      String overseasPurchased,             // [기본값] 해외구매대행여부: "OVERSEAS_PURCHASED" (PCC활성화)
      Boolean pccNeeded,                    // [기본값] 개인통관고유부호필수여부: true
      String externalVendorSku,             // [파라미터] 판매자상품코드: 자사 상품 고유 SKU
      List<Certification> certifications,   // [기본값] 상품인증정보: NOT_REQUIRED 세팅 목록
      List<String> searchTags,              // [파라미터] 검색어: 최대 20개 (SEO 최적화)
      List<Image> images,                   // [파라미터] 이미지목록: 썸네일 및 상세 이미지
      List<Notice> notices,                 // [파라미터] 상품고시정보: 카테고리별 고시정보 목록
      List<Attribute> attributes,           // [파라미터] 옵션목록: 쿠팡 카테고리 필수 속성
      List<Content> contents,               // [파라미터] 상세페이지: HTML 본문
      String offerCondition,                // [기본값] 상품상태: "NEW" (새상품 고정)
      String manufacture                    // [파라미터] 제조사: 브랜드명과 동일하게 주입
  ) {

    @Builder
    public record Certification(
        String certificationType,           // [기본값] 인증정보타입: "NOT_REQUIRED"
        String certificationCode            // [기본값] 인증코드: ""
    ) {}

    @Builder
    public record Image(
        Integer imageOrder,                 // [파라미터] 순서: 0(대표), 1, 2...
        String imageType,                   // [파라미터] 타입: "REPRESENTATION" 또는 "DETAIL"
        String vendorPath                   // [파라미터] 업체이미지경로: Hosted URL
    ) {}

    @Builder
    public record Notice(
        String noticeCategoryName,          // [파라미터] 고시정보카테고리: (ex. "건강기능식품")
        String noticeCategoryDetailName,    // [파라미터] 고시정보항목: (ex. "제품명")
        String content                      // [기본값] 내용: "상세페이지 참조" 고정
    ) {}

    @Builder
    public record Attribute(
        String attributeTypeName,           // [파라미터] 옵션타입명: Meta API 속성명
        String attributeValueName,          // [파라미터] 옵션값: 제품 스펙 (ex. "400mg")
        String exposed                      // [기본값] 필터구분: "NONE"
    ) {}

    @Builder
    public record Content(
        String contentsType,                // [기본값] 컨텐츠타입: "HTML" 고정
        List<ContentDetail> contentDetails  // [파라미터] 내부 텍스트 덩어리
    ) {
      @Builder
      public record ContentDetail(
          String content,                   // [파라미터] 내용: HTML 통짜 문자열
          String detailType                 // [기본값] 세부타입: "TEXT" 고정 (쿠팡 변태 규격)
      ) {}
    }
  }

  // =========================================================================
  // 🚀 [Factory Method] 기본값 자동 주입 및 Payload 완벽 조립 로직
  // =========================================================================
  public static CoupangProductPayload create(
      Product product,
      Long categoryCode,
      String masterName,
      String generalName,
      String brand,
      int salePrice,
      List<String> searchTags,
      List<Item.Image> images,
      List<Item.Notice> notices,
      List<Item.Attribute> attributes,
      String detailHtml
  ) {

    // 1. [기본값] 해외구매대행 인증 프리패스 정보 세팅
    Item.Certification defaultCert = Item.Certification.builder()
        .certificationType("NOT_REQUIRED")
        .certificationCode("")
        .build();

    // 2. [기본값+파라미터] 상세페이지 HTML 조립 (쿠팡 규격 HTML-TEXT)
    Item.Content.ContentDetail htmlDetail = Item.Content.ContentDetail.builder()
        .content(detailHtml)
        .detailType("TEXT")
        .build();

    Item.Content contentObj = Item.Content.builder()
        .contentsType("HTML")
        .contentDetails(List.of(htmlDetail))
        .build();

    // 3. [계산값] 건기식 통관 방어 (1인 최대 6병 룰)
    int bundleQty = (product.getLogisticsInfo() != null) ? product.getLogisticsInfo().getBundleQuantity() : 1;
    int safeMaxBuyForPerson = Math.max(1, 6 / bundleQty);

    // 4. [조립] 단일 Item(옵션) 객체 조립 (파라미터 + 고정정책 믹스)
    Item item = Item.builder()
        .itemName(bundleQty + "개")
        .originalPrice((int)(salePrice * 1.33))
        .salePrice(salePrice)
        .maximumBuyCount(999)
        .maximumBuyForPerson(safeMaxBuyForPerson)
        .maximumBuyForPersonPeriod(1)
        .outboundShippingTimeDay(3)
        .unitCount(0)
        .adultOnly("EVERYONE")
        .taxType("TAX")
        .parallelImported("NOT_PARALLEL_IMPORTED")
        .overseasPurchased("OVERSEAS_PURCHASED")
        .pccNeeded(true)
        .externalVendorSku(product.getSku())
        .certifications(List.of(defaultCert))
        .searchTags(searchTags)
        .images(images)
        .notices(notices)
        .attributes(attributes)
        .contents(List.of(contentObj))
        .offerCondition("NEW")
        .manufacture(brand)
        .build();

    // 5. [최종 조립] CoupangProductPayload (회사 전역 정책 하드코딩)
    return CoupangProductPayload.builder()
        .displayCategoryCode(categoryCode)
        .sellerProductName(masterName)
        .vendorId("A00213055")
        .saleStartedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
        .saleEndedAt("2099-12-31T23:59:59")
        .displayProductName(masterName)
        .brand(brand)
        .generalProductName(generalName)
        .deliveryMethod("AGENT_BUY")
        .deliveryCompanyCode("CJGLS")
        .deliveryChargeType("FREE")
        .deliveryCharge(0)
        .freeShipOverAmount(0)
        .deliveryChargeOnReturn(15000)
        .remoteAreaDeliverable("N")
        .unionDeliveryType("UNION_DELIVERY")
        .returnCenterCode("1000519746")
        .returnChargeName("서울 금천구")
        .companyContactNumber("010-2597-2480")
        .returnZipCode("08529")
        .returnAddress("서울특별시 금천구 시흥대로153길 90-4")
        .returnAddressDetail("103호")
        .returnCharge(15000)
        .outboundShippingPlaceCode(1206157)
        .vendorUserId("shouldbeshop")
        .requested(true)
        .items(List.of(item))
        .build();
  }
}