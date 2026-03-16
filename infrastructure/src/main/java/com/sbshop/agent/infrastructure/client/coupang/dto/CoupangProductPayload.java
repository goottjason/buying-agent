package com.sbshop.agent.infrastructure.client.coupang.dto;


import com.sbshop.agent.core.domain.product.model.Product;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🚀 외부 쿠팡 API로 전송할 JSON Payload (Request 명칭 지양)
 */
public record CoupangProductPayload(
    Long displayCategoryCode,
    String sellerProductName,
    String vendorId,
    String saleStartedAt,
    String saleEndedAt,
    String displayProductName,
    String brand,
    String generalProductName,
    String deliveryMethod,
    String deliveryCompanyCode,
    String deliveryChargeType,
    Integer deliveryCharge,
    Integer freeShipOverAmount,
    Integer deliveryChargeOnReturn,
    String remoteAreaDeliverable,
    String unionDeliveryType,
    String returnCenterCode,
    String returnChargeName,
    String companyContactNumber,
    String returnZipCode,
    String returnAddress,
    String returnAddressDetail,
    Integer returnCharge,
    Integer outboundShippingPlaceCode,
    String vendorUserId,
    Boolean requested,
    List<Item> items
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
      String itemName, Integer originalPrice, Integer salePrice, Integer maximumBuyCount,
      Integer maximumBuyForPerson, Integer maximumBuyForPersonPeriod, Integer outboundShippingTimeDay,
      Integer unitCount, String adultOnly, String taxType, String parallelImported,
      String overseasPurchased, Boolean pccNeeded, String externalVendorSku,
      List<Image> images, List<Notice> notices, List<Attribute> attributes, List<Content> contents
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

  public record Image(Integer imageOrder, String imageType, String vendorPath) {}
  public record Notice(String noticeCategoryName, String noticeCategoryDetailName, String content) {}
  public record Attribute(String attributeTypeName, String attributeValueName, String exposed) {}
  public record Content(String contentsType, List<ContentDetail> contentDetails) {}
  public record ContentDetail(String content, String detailType) {}
}