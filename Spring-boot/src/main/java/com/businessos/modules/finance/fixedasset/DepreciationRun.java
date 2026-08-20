package com.businessos.modules.finance.fixedasset;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One executed monthly depreciation run - its existence is the idempotency guard
 * (a month can only be run once per company).
 */
@Entity
@Table(name = "depreciation_runs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "run_year", "run_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRun extends BaseEntity {

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "run_year", nullable = false)
    private int year;

    @Column(name = "run_month", nullable = false)
    private int month; // 1-12 calendar month

    private BigDecimal totalAmount;
    private int assetsDepreciated;
    private String runBy;
    private LocalDateTime runAt;
}
