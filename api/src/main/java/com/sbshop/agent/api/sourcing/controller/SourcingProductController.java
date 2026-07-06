package com.sbshop.agent.api.sourcing.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import com.sbshop.agent.core.domain.sourcing.repository.ProductSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import com.sbshop.agent.core.domain.common.enums.EntityStatus;

@Slf4j
@RestController
@RequestMapping("/api/v1/sourcing-products")
@RequiredArgsConstructor
public class SourcingProductController {

    private final ProductSourceRepository repository;

    @GetMapping
    public CommonResponse<List<SourcingProductResponse>> getSourcingProducts() {
        List<ProductSource> sources = repository.findByStatusOrderByIdDesc(EntityStatus.ACTIVE);
        
        List<SourcingProductResponse> responses = sources.stream().map(source -> new SourcingProductResponse(
            source.getId(),
            source.getSourcingSite().getSiteCode().name(),
            source.getProduct().getName(),
            source.getLastScrapedPrice() != null ? source.getLastScrapedPrice().toString() : "0",
            source.getLastScrapedPrice() != null ? source.getLastScrapedPrice().multiply(java.math.BigDecimal.valueOf(1400)).toString() : "0", // 임시 타겟가
            source.getLastScrapedStockStatus() != null ? source.getLastScrapedStockStatus().name() : "OUT_OF_STOCK",
            source.getLastSyncTime() != null ? source.getLastSyncTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-",
            source.getProduct().getImageInfo() != null && !source.getProduct().getImageInfo().getHostedImages().isEmpty() 
                ? source.getProduct().getImageInfo().getHostedImages().get(0) 
                : "https://via.placeholder.com/40"
        )).collect(Collectors.toList());

        return CommonResponse.ok(responses);
    }

    public record SourcingProductResponse(
        Long id,
        String site,
        String name,
        String sourcePrice,
        String targetPrice,
        String stock,
        String lastSync,
        String imageUrl
    ) {}
}
