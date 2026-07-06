package com.sbshop.agent.core.domain.sourcing.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "margin_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
@SQLDelete(sql = "UPDATE margin_policies SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class MarginPolicy extends BaseEntity {

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal exchangeRate;

    @Column(name = "shipping_fee", nullable = false)
    private int shippingFee;

    @Column(name = "margin_rate_percent", nullable = false)
    private int marginRatePercent;

    @Column(name = "commission_rate_percent", nullable = false)
    private int commissionRatePercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sourcing_site_id")
    private SourcingSite sourcingSite;

    @Builder
    public MarginPolicy(String policyName, BigDecimal exchangeRate, int shippingFee, int marginRatePercent, int commissionRatePercent, SourcingSite sourcingSite) {
        this.policyName = policyName;
        this.exchangeRate = exchangeRate;
        this.shippingFee = shippingFee;
        this.marginRatePercent = marginRatePercent;
        this.commissionRatePercent = commissionRatePercent;
        this.sourcingSite = sourcingSite;
    }
}
