package com.sbshop.agent.infrastructure.client.sourcing.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbshop.agent.core.application.sourcing.dto.ScrapedProductDto;
import com.sbshop.agent.core.domain.product.model.enums.MeasureUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class IherbProductParser {

  private final ObjectMapper objectMapper;

  /**
   * 아이허브 JSON 문자열을 받아 순수 도메인 객체로 조립합니다.
   */
  public ScrapedProductDto parse(String originUrl, String json) {
    try {
      JsonNode root = objectMapper.readTree(json);

      // 1. 기본 정보 파싱
      String brand = root.path("brandName")
          .asText("");
      String baseName = root.path("displayName")
          .asText("");
      String originalName = extractEngNameRule(root, brand);
      String origin = root.path("countryOfOrigin")
          .asText("미국");
      String weight = root.path("weightKg")
          .isMissingNode() ?
          root.path("actualWeight")
              .asText("") :
          root.path("weightKg")
              .asText("");
      String expirationDate = root.path("formattedExpirationDate")
          .asText("");
      boolean isAvailable = root.path("isAvailableToPurchase")
          .asBoolean(false);
      String rawCategory = extractCategoryRule(root);

      // 2. 가격 파싱 (할인가 우선, 없으면 정상가)
      int costPrice = root.path("discountPriceAmount")
          .asInt(0) > 0 ? root.path("discountPriceAmount")
          .asInt() : root.path("listPriceAmount")
          .asInt(0);

      // 3. 용량(Capacity)과 단위(MeasureUnit) 추출 로직
      UnitInfo unitInfo = extractUnitInfo(root);
      int capacity = unitInfo.capacity();
      MeasureUnit measureUnit = unitInfo.unit();

      // 4. 이미지 추출 (최대 3장 추가)
      List<String> sourceImages = extractImages(root);

      // 5. 아이허브 원본 HTML 조립 (상세 정보 덩어리)
      String rawSourceHtml = String.format(
          "<div>%s</div><br/><h3>복용법</h3><div>%s</div><br/><h3>성분</h3><div>%s</div><br/><h3>주의사항</h3><div>%s</div>",
          root.path("description").asText(""), root.path("suggestedUse").asText(""),
          root.path("ingredients").asText(""), root.path("warnings").asText("")
      );

      // 6. DTO 조립 후 반환 (어떠한 비즈니스 로직도 개입하지 않음!)
      return ScrapedProductDto.builder()
          .sourceUrl(originUrl)
          .costPrice(costPrice)
          .baseName(baseName)
          .originalName(originalName)
          .brand(brand)
          .origin(origin)
          .weight(weight)
          .expirationDate(expirationDate)
          .capacity(capacity)
          .measureUnit(measureUnit)
          .sourceImages(sourceImages)
          .rawSourceHtml(rawSourceHtml)
          .isAvailable(isAvailable)
          .rawCategory(rawCategory)
          .build();

    } catch (Exception e) {
      log.error("아이허브 JSON 파싱 중 오류 발생. URL: {}", originUrl, e);
      throw new RuntimeException("아이허브 JSON 파싱 중 오류 발생", e);
    }
  }

  // =====================================================================
  // 🛠️ 파싱 보조 메서드들 (기존 로직 유지)
  // =====================================================================

  private record UnitInfo(int capacity, MeasureUnit unit) {}

  private UnitInfo extractUnitInfo(JsonNode root) {
    String packageQty = root.path("packageQuantity").asText(""); // 예: "180 개"
    String displayName = root.path("displayName").asText("");    // 예: "... 180정(1정당 100mg)"
    String urlName = root.path("urlName").asText("");            // 예: "...-180-tablets-..."

    // 1. packageQuantity에서 순수 숫자(Capacity)만 추출
    int capacity = 1;
    Matcher numMatcher = Pattern.compile("(\\d+)").matcher(packageQty);
    if (numMatcher.find()) {
      capacity = Integer.parseInt(numMatcher.group(1));
    }

    MeasureUnit unit = MeasureUnit.UNKNOWN;

    // 2. 한글 상품명(displayName)에서 '추출한 숫자 + 단위' 패턴 검사
    // 정규식 설명: 180 뒤에 공백이 있을수도 없을수도 있고, 그 뒤에 정/캡슐 등이 오는지 검사
    Pattern krPattern = Pattern.compile(capacity + "\\s*(정|타블렛|캡슐|소프트젤|베지\\s*캡슐|개|팩|병|mg|g|ml|oz)", Pattern.CASE_INSENSITIVE);
    Matcher krMatcher = krPattern.matcher(displayName);
    if (krMatcher.find()) {
      unit = mapKoreanUnit(krMatcher.group(1).replace(" ", ""));
    }

    // 3. 한글에서 못 찾았다면, 영문 슬러그(urlName)에서 교차 검사 (가장 정확한 영문 데이터 활용)
    if (unit == MeasureUnit.UNKNOWN) {
      Pattern enPattern = Pattern.compile(capacity + "-(tablets?|capsules?|softgels?|v-caps?|veggie-caps?|oz|ml|g|mg)", Pattern.CASE_INSENSITIVE);
      Matcher enMatcher = enPattern.matcher(urlName);
      if (enMatcher.find()) {
        unit = mapEnglishUnit(enMatcher.group(1));
      }
    }

    // 4. 그래도 못 찾았으면 기본값 폴백 (Fall-back)
    if (unit == MeasureUnit.UNKNOWN) {
      String lowerQty = packageQty.toLowerCase();
      if (lowerQty.contains("g")) unit = MeasureUnit.G;
      else if (lowerQty.contains("ml")) unit = MeasureUnit.ML;
      else unit = MeasureUnit.EA; // 최후의 수단
    }

    return new UnitInfo(capacity, unit);
  }

  private MeasureUnit mapKoreanUnit(String str) {
    if (str.contains("정") || str.contains("타블렛")) return MeasureUnit.TABLET;
    if (str.contains("캡슐") || str.contains("소프트젤")) return MeasureUnit.CAPSULE;
    if (str.contains("병")) return MeasureUnit.BOTTLE;
    if (str.contains("팩")) return MeasureUnit.PACK;
    if (str.contains("mg")) return MeasureUnit.MG;
    if (str.contains("g")) return MeasureUnit.G;
    if (str.contains("ml")) return MeasureUnit.ML;
    if (str.contains("oz")) return MeasureUnit.OZ;
    return MeasureUnit.EA;
  }

  private MeasureUnit mapEnglishUnit(String str) {
    if (str.contains("tablet")) return MeasureUnit.TABLET;
    if (str.contains("capsule") || str.contains("softgel") || str.contains("cap")) return MeasureUnit.CAPSULE;
    if (str.contains("mg")) return MeasureUnit.MG;
    if (str.contains("g")) return MeasureUnit.G;
    if (str.contains("ml")) return MeasureUnit.ML;
    if (str.contains("oz")) return MeasureUnit.OZ;
    return MeasureUnit.UNKNOWN;
  }

  private List<String> extractImages(JsonNode root) {
    String partNumber = root.path("partNumber")
        .asText(null);
    JsonNode indices = root.path("imageIndices");
    if (partNumber == null || indices.isMissingNode() || !indices.isArray()) {
      return List.of();
    }

    String brandLike = partNumber.split("-")[0].toLowerCase();
    String part = partNumber.toLowerCase()
        .replace("-", "");

    List<String> links = new ArrayList<>();
    int count = 0;
    for (JsonNode idxNode : indices) {
      if (count >= 5) {
        break; // 최대 5장 (메인 1 + 추가 4)
      }
      links.add(String.format(
          "https://cloudinary.images-iherb.com/image/upload/f_auto,q_auto:eco/images/%s/%s/l/%d.jpg",
          brandLike, part, idxNode.asInt()
      ));
      count++;
    }
    return links;
  }

  private String extractEngNameRule(JsonNode root, String brandName) {
    String urlName = root.path("urlName")
        .asText(null);
    if (urlName == null) {
      return brandName;
    }

    String engBrand = brandName.contains("(") ? brandName.substring(0, brandName.indexOf("("))
        .trim() : brandName;
    String brandSlug = engBrand.toLowerCase()
        .replaceAll("[^a-z0-9]", "-")
        .replaceAll("-+", "-");

    String url = urlName.toLowerCase();
    if (url.startsWith(brandSlug + "-")) {
      url = url.substring(brandSlug.length() + 1);
    }

    String[] tokens = url.split("-");
    List<String> parts = new ArrayList<>();
    List<String> units = new ArrayList<>();
    Set<String> unitSet = Set.of(
        "mg", "g", "ml", "oz", "iu", "softgels", "tablets", "capsules", "fl", "lb", "lbs");

    for (int i = 0; i < tokens.length; i++) {
      if (i + 1 < tokens.length && tokens[i].matches("\\d+") && unitSet.contains(tokens[i + 1])) {
        units.add(String.format("%,d", Integer.parseInt(tokens[i])) + " " + StringUtils.capitalize(
            tokens[i + 1]));
        i++;
      } else if (tokens[i].matches("\\d+")) {
        units.add(String.format("%,d", Integer.parseInt(tokens[i])));
      } else if (unitSet.contains(tokens[i])) {
        units.add(StringUtils.capitalize(tokens[i]));
      } else if (!tokens[i].isBlank()) {
        parts.add(StringUtils.capitalize(tokens[i]));
      }
    }

    StringBuilder sb = new StringBuilder(engBrand);
    if (!parts.isEmpty()) {
      sb.append(", ")
          .append(String.join(" ", parts));
    }
    if (!units.isEmpty()) {
      sb.append(", ")
          .append(units.get(0));
      if (units.size() > 1) {
        sb.append(" (")
            .append(String.join(", ", units.subList(1, units.size())))
            .append(")");
      }
    }
    return sb.toString()
        .replaceAll(" ,", ",")
        .replaceAll("  +", " ")
        .trim();
  }

  private String extractCategoryRule(JsonNode root) {
    JsonNode paths = root.path("canonicalPaths");
    if (paths.isArray()) {
      for (JsonNode pathList : paths) {
        for (JsonNode pathNode : pathList) {
          String name = pathNode.path("displayName")
              .asText("");
          if ("보충제".equals(name)) {
            return "보충제";
          }
          if ("스포츠".equals(name)) {
            return "스포츠";
          }
        }
      }
    }
    return "식료품";
  }
}