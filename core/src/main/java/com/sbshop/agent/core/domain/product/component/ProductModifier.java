package com.sbshop.agent.core.domain.product.component;

import com.sbshop.agent.core.domain.product.dto.ProductBulkUpdateCommand;
import com.sbshop.agent.core.domain.product.dto.ProductSaveCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductModifier {
  private final ProductFinder productFinder; // 단건 조회를 위해 Finder 주입 (★ 상단에 추가)
  private final ProductRepository productRepository;

  // ★ 일괄 수정
  @Transactional
  public int bulkUpdate(ProductBulkUpdateCommand command) {
    // 1. 대상 상품들을 한 번에 조회합니다. (findBySkuIn 같은 쿼리메서드가 필요합니다)
    // JPA 인터페이스에: List<Product> findBySkuIn(List<String> skus); 추가 필요!
    List<Product> products = productRepository.findBySkuIn(command.getSkus());

    // 2. 각각의 상품 정보를 업데이트합니다. (JPA의 더티 체킹 덕분에 save()를 안 해도 DB에 반영됩니다!)
    for (Product product : products) {
      product.updateBulkInfo(
          command.getCategory(),
          command.getSearchKeywords(),
          command.getMemo()
      );
    }

    return products.size(); // 성공한 건수 반환
  }

  // ★ 일괄 삭제 (소프트 삭제)
  @Transactional
  public int bulkDelete(List<String> skus) {
    List<Product> products = productRepository.findBySkuIn(skus);

    for (Product product : products) {
      product.delete();
    }

    return products.size();
  }

  // ★ 단건 등록 (POST)
  /*@Transactional
  public String createProduct(ProductSaveCommand command) {
    // 이미 존재하는 SKU인지 검증
    if (productRepository.existsBySku(command.getSku())) {
      throw new IllegalArgumentException("이미 존재하는 SKU입니다: " + command.getSku());
    }

    // 새로운 상품 엔티티 생성 (모달에서 입력받은 최소 정보만 우선 세팅)
    Product newProduct = Product.builder()
        .sku(command.getSku())
        .name(command.getName())
        .category(command.getCategory())
        .priceInfo(PriceInfo.builder().salePrice(command.getSalePrice()).build())
        .memo(command.getMemo())
        .detailHtml(command.getDetailHtml())
        .build();

    productRepository.save(newProduct);
    return newProduct.getSku();
  }*/

  // ★ 단건 수정 (PUT)
  /*@Transactional
  public void updateProduct(String sku, ProductSaveCommand command) {
    // Finder를 이용해 기존 상품을 가져옵니다. (없으면 에러)
    Product product = productFinder.getBySku(sku);

    // 엔티티 스스로 값을 변경하도록 지시합니다. (JPA 더티 체킹 덕분에 자동으로 UPDATE 쿼리 발생)
    product.updateDetail(
        command.getName(),
        command.getCategory(),
        command.getSalePrice(),
        command.getMemo(),
        command.getDetailHtml()
    );
  }*/

  // ★ 엑셀 인라인 에디팅 일괄 저장 로직
  // ★ 엑셀 인라인 에디팅 일괄 저장 로직
  @Transactional
  public void bulkEditProducts(List<ProductSaveCommand> commands) {
    for (ProductSaveCommand command : commands) {
      Product product = productFinder.getBySku(command.getSku());
      // 찾아온 엔티티에게 "프론트에서 넘어온 데이터로 싹 다 덮어써!" 라고 명령합니다.
      product.updateAllFields(command);
    }
  }
}