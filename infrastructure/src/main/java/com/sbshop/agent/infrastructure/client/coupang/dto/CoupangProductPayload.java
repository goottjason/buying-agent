package com.sbshop.agent.infrastructure.client.coupang.dto;


import com.sbshop.agent.core.domain.product.model.Product;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🚀 외부 쿠팡 API로 전송할 JSON Payload (Request 명칭 지양)
 */
public record CoupangProductPayload(
    Long displayCategoryCode,               // 노출카테고리코드:
    String sellerProductName,               // 등록상품명: 공통 상품명, ex. KAL 마그네슘 타우레이트 플러스 400mg 90정 2개
    String vendorId,                        // 판매자ID: A00213055
    String saleStartedAt,                   // 판매시작일시: LocalDateTime.now()
    String saleEndedAt,                     // 판매종료일시: 2099-12-31T23:59:59
    String displayProductName,              // 노출상품명: 브랜드 제품명 함량 수량, ex. KAL 마그네슘 타우레이트 플러스 400mg 90정 2개
    String brand,                           // 브랜드명: 띄어쓰기X, 특수문자X
    String generalProductName,              // 제품명: 제품명 함량


    String deliveryMethod,                  // 배송방법: AGENT_BUY(구매대행)
    String deliveryCompanyCode,             // 택배사코드: CJGLS
    String deliveryChargeType,              // 배송비종류: FREE(무료)
    Integer deliveryCharge,                 // 기본배송비: 0
    Integer freeShipOverAmount,             // 조건부무료배송기준금액: 0
    Integer deliveryChargeOnReturn,         // 초도반품배송비: 15000
    String remoteAreaDeliverable,           // 도서산간배송여부: N (해외배송은 까다로움)
    String unionDeliveryType,               // 묶음배송여부: UNION_DELIVERY(가능)
    String returnCenterCode,                // 반품지센터코드: 1000519746 (반드시 국내 주소 코드)
    String returnChargeName,                // 반품지명: 서울 금천구
    String companyContactNumber,            // 반품지연락처: 010-2597-2480
    String returnZipCode,                   // 반품지우편번호: 08529
    String returnAddress,                   // 반품지기본주소: 서울특별시 금천구 시흥대로153길 90-4 (가산동)
    String returnAddressDetail,             // 반품지상세주소: 103호
    Integer returnCharge,                   // 반품회수편도배송비: 15000 (초도반품배송비의 100~150% 룰)
    Integer outboundShippingPlaceCode,      // 출고지주소코드: 1206157 (반드시 해외 주소 코드)
    String vendorUserId,                    // 실사용자로그인ID: shouldbeshop
    Boolean requested,                      // 자동승인요청여부: true
    List<Item> items                        // 옵션목록
) {
  // =================================================================
  // 💡 생성의 복잡성을 숨기는 정적 팩토리 메서드
  // =================================================================
  public static CoupangProductPayload create(Product product, Long categoryId, List<Item> items) {
    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    String sellerName = product.getBrand() + " " + product.getBaseName();

    return new CoupangProductPayload(
        categoryId, sellerName, "A00213055", // vendorId
        now, "2099-12-31T23:59:59",         // 판매기간
        sellerName, product.getBrand(), product.getBaseName(),
        "AGENT_BUY", "CJGLS", "FREE",       // 구매대행, CJ대한통운, 무료배송
        0, 0, 5000, "N", "UNION_DELIVERY",  // 배송비 및 도서산간 설정
        "1000519746", "서울 금천구", "010-2597-2480", "08529", // 반품지
        "서울특별시 금천구 시흥대로153길 90-4 (가산동)", "103호", 5000,
        1206157, "shouldbeshop", true, items
    );
  }

  public record Item(
      String itemName,                      // 업체상품옵션명: ex. 90정 2개
      Integer originalPrice,                // 정가: 최종판매가보다 33% 높게 가상의 정가를 세팅
      Integer salePrice,                    // 최종판매가
      Integer maximumBuyCount,              // 판매가능수량: 34
      Integer maximumBuyForPerson,          // 인당최대구매수량: 6÷묶음수량 (건기식 공식), 0 (그 외)
      Integer maximumBuyForPersonPeriod,    // 인당최대구매수량제한기간: 1 (입항일기준)
      Integer outboundShippingTimeDay,      // 기준출고일: 5
      Integer unitCount,                    // 단위수량: 180 (90*2), "1정당 216원" 형태로 예쁘게 노출, 0 (비노출) 또는 180
      String adultOnly,                     // 성인여부: EVERYONE
      String taxType,                       // 과세여부: TAX
      String parallelImported,              // 병행수입여부: NOT_PARALLEL_IMPORTED
      String overseasPurchased,             // 해외구매대행여부: OVERSEAS_PURCHASED
      Boolean pccNeeded,                    // 개인통관고유부호필수여부: true
      String externalVendorSku,             // 판매자상품코드: SKU
      List<Certification> certifications,   // 상품인증정보
      List<SearchTag> searchTags,           // 검색어
      List<Image> images,                   //
      List<Notice> notices,
      List<Attribute> attributes,
      List<Content> contents
  ) {
    public static Item create(Product product, List<Image> images, List<Notice> notices, List<Attribute> attributes, List<Content> contents) {
      int price = product.getPriceInfo().getSalePrice().intValue();
      int qty = product.getLogisticsInfo().getBundleQuantity();
      return new Item(
          qty + "개", price, price, 999, 0, 1, 5, qty,
          "EVERYONE", "TAX", "NOT_PARALLEL_IMPORTED", "OVERSEAS_PURCHASED", true,
          product.getSku(), images, notices, attributes, contents
      );
    }
  }
  public record Certification(
      String certificationType,             // 인증정보타입: NOT_REQUIRED
      String certificationCode,             // 인증코드: ""
      String certificationValue
  ) {
    public static Certification create(String certificationName, String certificationValue) {
      return new Certification(certificationName, certificationValue);
    }
  }
  public record SearchTag(String searchTagName) {}
  public record Image(Integer imageOrder, String imageType, String vendorPath) {}
  public record Notice(String noticeCategoryName, String noticeCategoryDetailName, String content) {}
  public record Attribute(String attributeTypeName, String attributeValueName, String exposed) {}
  public record Content(String contentsType, List<ContentDetail> contentDetails) {}
  public record ContentDetail(String content, String detailType) {}
}