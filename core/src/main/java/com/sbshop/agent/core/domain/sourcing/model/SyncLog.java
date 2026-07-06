package com.sbshop.agent.core.domain.sourcing.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sync_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncLog extends BaseEntity {

    @Column(name = "site_code", nullable = false, length = 50)
    private String siteCode;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sync_type", nullable = false, length = 50)
    private String syncType; // PRICE, STOCK, IMAGE, PRICE_STOCK

    @Column(name = "sync_status", nullable = false, length = 50)
    private String syncStatus; // SUCCESS, FAIL, CRAWL_ERROR

    @Column(name = "message", length = 2000)
    private String message;

    @Builder
    public SyncLog(String siteCode, String productName, String syncType, String syncStatus, String message) {
        this.siteCode = siteCode;
        this.productName = productName;
        this.syncType = syncType;
        this.syncStatus = syncStatus;
        this.message = message;
    }
}
