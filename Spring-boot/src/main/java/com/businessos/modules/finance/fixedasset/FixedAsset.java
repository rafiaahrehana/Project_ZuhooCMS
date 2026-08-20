package com.businessos.modules.finance.fixedasset;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A capitalized asset (equipment, furniture, vehicles...) depreciated straight-line
 * over its useful life. Registering one posts Dr Fixed Assets / Cr Cash; each monthly
 * depreciation run posts Dr Depreciation Expense / Cr Accumulated Depreciation.
 * Previously buying a laptop either hit Operating Expenses in full (wrong for a
 * multi-year asset) or never touched the books at all (the ITAM module).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "fixed_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FixedAsset extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(nullable = false)
    private String name;

    private String assetTag;
    private String category; // e.g. "Computers", "Vehicles" - informational

    @Column(nullable = false)
    private BigDecimal cost;

    @Builder.Default
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private int usefulLifeMonths;

    private LocalDate acquisitionDate;

    @Builder.Default
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FixedAssetStatus status = FixedAssetStatus.ACTIVE;

    private String notes;

    public BigDecimal depreciableBase() {
        BigDecimal salvage = salvageValue != null ? salvageValue : BigDecimal.ZERO;
        return cost.subtract(salvage).max(BigDecimal.ZERO);
    }

    /** Straight-line monthly charge, capped so accumulated never exceeds the depreciable base. */
    public BigDecimal monthlyDepreciation() {
        if (usefulLifeMonths <= 0) return BigDecimal.ZERO;
        BigDecimal monthly = depreciableBase().divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = depreciableBase().subtract(
                accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO);
        return monthly.min(remaining.max(BigDecimal.ZERO));
    }

    public BigDecimal bookValue() {
        return cost.subtract(accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO);
    }
}
