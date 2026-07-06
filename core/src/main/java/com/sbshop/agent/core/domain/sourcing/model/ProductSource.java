package com.sbshop.agent.core.domain.sourcing.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.product.model.Product;
import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_sources", indexes = {
    @Index(name = "idx_source_product_code", columnList = "source_product_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
@SQLDelete(sql = "UPDATE product_sources SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class ProductSource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sourcing_site_id", nullable = false)
    private SourcingSite sourcingSite;

    @Column(name = "source_product_code", nullable = false, length = 100)
    private String sourceProductCode;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "last_scraped_price", precision = 15, scale = 2)
    private BigDecimal lastScrapedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_scraped_stock_status", length = 50)
    private StockStatus lastScrapedStockStatus;

    @Column(name = "last_scraped_image_hash", length = 255)
    private String lastScrapedImageHash;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Builder
    public ProductSource(Product product, SourcingSite sourcingSite, String sourceProductCode, String sourceUrl, 
                         BigDecimal lastScrapedPrice, StockStatus lastScrapedStockStatus, String lastScrapedImageHash) {
        this.product = product;
        this.sourcingSite = sourcingSite;
        this.sourceProductCode = sourceProductCode;
        this.sourceUrl = sourceUrl;
        this.lastScrapedPrice = lastScrapedPrice;
        this.lastScrapedStockStatus = lastScrapedStockStatus;
        this.lastScrapedImageHash = lastScrapedImageHash;
    }
}
