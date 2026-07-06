package com.sbshop.agent.core.domain.sourcing.model;

import com.sbshop.agent.core.domain.common.BaseEntity;
import com.sbshop.agent.core.domain.sourcing.model.enums.SourcingSiteCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "sourcing_sites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("status = 'ACTIVE'")
@SQLDelete(sql = "UPDATE sourcing_sites SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class SourcingSite extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "site_code", nullable = false, unique = true, length = 50)
    private SourcingSiteCode siteCode;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "default_currency", length = 10)
    private String defaultCurrency;

    @Builder
    public SourcingSite(SourcingSiteCode siteCode, String baseUrl, String defaultCurrency) {
        this.siteCode = siteCode;
        this.baseUrl = baseUrl;
        this.defaultCurrency = defaultCurrency;
    }
}
