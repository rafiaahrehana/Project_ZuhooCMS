package com.businessos.shared.payment.wallet;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "tenant_credits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantCredit extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal amount;

    @Column(nullable = false)
    private String reason; // REFUND, REFERRAL_REWARD, PROMO, COMPENSATION

    private LocalDate expiresAt;
    @Builder.Default
    private boolean used = false;
    private LocalDateTime usedAt;
}
