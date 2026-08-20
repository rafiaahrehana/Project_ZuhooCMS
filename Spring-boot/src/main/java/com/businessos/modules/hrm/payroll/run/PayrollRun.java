package com.businessos.modules.hrm.payroll.run;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The payroll batch header from the spec: one run per company per period,
 * grouping the per-employee Payroll rows, carrying frozen totals and the
 * approval workflow. Once a run passes PENDING_APPROVAL its lines lock -
 * see PayrollServiceImpl.assertLineEditable.
 *
 * DRAFT -> CALCULATED -> PENDING_APPROVAL -> APPROVED -> PAID
 * plus REJECTED (from pending) and CANCELLED (before approval).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "payroll_runs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "pay_month", "pay_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRun extends BaseEntity {

    public enum RunStatus { DRAFT, CALCULATED, PENDING_APPROVAL, APPROVED, PAID, REJECTED, CANCELLED }

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 30)
    private String runNumber;

    @Column(name = "pay_month", nullable = false)
    private int payMonth;

    @Column(name = "pay_year", nullable = false)
    private int payYear;

    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate paymentDate;

    @Builder.Default
    private Integer totalEmployees = 0;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal totalDeduction = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private RunStatus status = RunStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private Long createdById;
    private Long approvedById;
    private LocalDateTime approvedAt;
}
