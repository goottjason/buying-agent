package com.sbshop.agent.api.sourcing.controller;

import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.dto.ProductCreateCommand;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.product.repository.ProductRepository;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import com.sbshop.agent.core.domain.sourcing.model.SourcingSite;
import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;
import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import com.sbshop.agent.core.domain.sourcing.repository.ProductSourceRepository;
import com.sbshop.agent.core.domain.sourcing.repository.SourcingSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevSeedController {

    private final SourcingSiteRepository siteRepository;
    private final ProductRepository productRepository;
    private final ProductSourceRepository productSourceRepository;

    @PostMapping("/seed")
    @Transactional
    public String seedData() {
        log.info("개발용 샘플 데이터 시딩(Seed) 시작...");

        // 1. 소싱 사이트 생성
        SourcingSite iherbSite = SourcingSite.builder()
                .siteCode(SourcingSiteCode.IHERB)
                .baseUrl("https://www.iherb.com")
                .defaultCurrency("USD")
                .build();
        
        SourcingSite amazonSite = SourcingSite.builder()
                .siteCode(SourcingSiteCode.AMAZON_US)
                .baseUrl("https://www.amazon.com")
                .defaultCurrency("USD")
                .build();
        
        siteRepository.saveAll(List.of(iherbSite, amazonSite));

        // 2. 상품(Product) 생성
        ProductCreateCommand cmd1 = ProductCreateCommand.builder()
                .brand("California Gold Nutrition")
                .baseName("Omega-3")
                .originalName("California Gold Nutrition, Omega-3")
                .rawCategory("영양제")
                .costPrice(BigDecimal.valueOf(12500))
                .isAvailable(true)
                .capacity(BigDecimal.valueOf(180))
                .measureUnit(com.sbshop.agent.core.domain.product.model.enums.MeasureUnit.CAPSULE)
                .sourceImages(List.of("https://kr.iherb.com/pr/california-gold-nutrition-omega-3-premium-fish-oil-180-epa-120-dha-100-fish-gelatin-softgels/62118"))
                .hostedImages(List.of("https://via.placeholder.com/40"))
                .sourceUrl("https://kr.iherb.com/pr/california-gold-nutrition-omega-3-premium-fish-oil-180-epa-120-dha-100-fish-gelatin-softgels/62118")
                .vendor(com.sbshop.agent.core.domain.product.model.enums.VendorType.IHB)
                .origin("미국")
                .build();
        Product p1 = Product.create("IHERB-001", cmd1);

        ProductCreateCommand cmd2 = ProductCreateCommand.builder()
                .brand("Razer")
                .baseName("DeathAdder V2 Gaming Mouse")
                .originalName("Razer DeathAdder V2 Gaming Mouse")
                .rawCategory("마우스")
                .costPrice(BigDecimal.valueOf(68000))
                .isAvailable(true)
                .capacity(BigDecimal.valueOf(1))
                .measureUnit(com.sbshop.agent.core.domain.product.model.enums.MeasureUnit.UNKNOWN)
                .sourceImages(List.of("https://www.amazon.com/dp/B082G5SPR5"))
                .hostedImages(List.of("https://via.placeholder.com/40"))
                .sourceUrl("https://www.amazon.com/dp/B082G5SPR5")
                .vendor(com.sbshop.agent.core.domain.product.model.enums.VendorType.AMS)
                .origin("미국")
                .build();
        Product p2 = Product.create("AMZN-001", cmd2);
                
        productRepository.saveAll(List.of(p1, p2));

        // 3. ProductSource 생성
        ProductSource ps1 = ProductSource.builder()
                .product(p1)
                .sourcingSite(iherbSite)
                .sourceProductCode("62118")
                .sourceUrl("https://kr.iherb.com/pr/california-gold-nutrition-omega-3-premium-fish-oil-180-epa-120-dha-100-fish-gelatin-softgels/62118")
                .lastScrapedPrice(BigDecimal.valueOf(7.00))
                .lastScrapedStockStatus(StockStatus.IN_STOCK)
                .build();

        ProductSource ps2 = ProductSource.builder()
                .product(p2)
                .sourcingSite(amazonSite)
                .sourceProductCode("B082G5SPR5")
                .sourceUrl("https://www.amazon.com/dp/B082G5SPR5")
                .lastScrapedPrice(BigDecimal.valueOf(39.99))
                .lastScrapedStockStatus(StockStatus.IN_STOCK)
                .build();

        productSourceRepository.saveAll(List.of(ps1, ps2));

        log.info("샘플 데이터 생성 성공!");
        return "SUCCESS";
    }
}
