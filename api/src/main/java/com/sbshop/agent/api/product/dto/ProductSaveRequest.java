package com.sbshop.agent.api.product.dto;

import com.sbshop.agent.core.domain.product.dto.ProductCreateCommand;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import com.sbshop.agent.core.domain.product.model.enums.VendorType;
import java.math.BigDecimal;
import java.util.List;

public record ProductSaveRequest(
    String sourceUrl,      // 💡 originUrl -> sourceUrl 통일
    int costPrice,         // 💡 originPrice -> costPrice 통일
    String baseName,
    String originalName,
    String brand,
    String origin,         // 💡 originCountry -> origin 통일
    String weight,
    String expirationDate,
    int capacity,
    MeasureUnit measureUnit,
    List<String> sourceImages,
    String rawSourceHtml,
    boolean isAvailable,
    int bundleQuantity,    // 💡 bundleCount -> bundleQuantity 통일
    int marginRate,
    String rawCategory     // 💡 category -> rawCategory 통일 (아이허브 원본 문자열)
) {

  // 🚀 컨트롤러에서 호출하여 UseCase로 넘길 Command 생성
  public ProductCreateCommand toCommand() {
    return ProductCreateCommand.builder()
        .sourceUrl(sourceUrl)
        .costPrice(BigDecimal.valueOf(costPrice))
        .baseName(baseName)
        .originalName(originalName)
        .brand(brand)
        .origin(origin)
        // "0.25 kg" 같은 문자열에서 숫자만 쏙 빼서 안전하게 BigDecimal로 변환 방어!
        .weight(weight != null ? new BigDecimal(weight.replaceAll("[^0-9.]", "")) : BigDecimal.ZERO)
        .expirationDate(expirationDate)
        .capacity(BigDecimal.valueOf(capacity))
        .measureUnit(measureUnit)
        .sourceImages(sourceImages)
        .rawSourceHtml(rawSourceHtml)
        .isAvailable(isAvailable)
        .bundleQuantity(bundleQuantity)
        .marginRate(BigDecimal.valueOf(marginRate))
        .rawCategory(rawCategory)
        .vendor(VendorType.IHB) // 💡 기본 소싱처(아이허브) 세팅
        .build();
  }
}