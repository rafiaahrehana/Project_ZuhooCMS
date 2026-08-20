package com.businessos.modules.hrm.payroll.settings;

import com.businessos.core.base.BaseEntity;
import com.businessos.enums.PerDayBasis;
import com.businessos.enums.SalaryBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One company's payroll policy: how a day's pay is derived, what absence costs,
 * whether overtime is paid, and the default split of a salary into components.
 *
 * Exists as its own table rather than more columns on Company because these are
 * HR policy rather than company identity, and they carry their own permission.
 *
 * Every default here reproduces the behaviour that was hardcoded before this
 * entity existed, so an existing tenant sees no change to its payroll until an
 * owner deliberately edits something.
 */
@Entity
@Table(name = "payroll_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollSettings extends BaseEntity {

    /** One row per company. */
    @Column(nullable = false, unique = true)
    private Long companyId;

    // ── Absence ──────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PerDayBasis perDayBasis = PerDayBasis.CALENDAR_DAYS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalaryBase absenceDeductionBase = SalaryBase.GROSS;

    // ── Overtime ─────────────────────────────────────────────

    /**
     * Off by default: salaried engineers in most IT companies are not paid
     * overtime, they take time off in lieu. Tenants running shift-based roles
     * turn it on.
     */
    @Builder.Default
    private boolean overtimeEnabled = false;

    /**
     * Premium applied to the ordinary hourly rate. The Bangladesh Labour Act
     * 2006 (s.108) requires twice the ordinary rate, hence the default; a
     * tenant under different rules can lower it.
     */
    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal overtimeMultiplier = new BigDecimal("2.00");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalaryBase overtimeBase = SalaryBase.BASIC;

    /** Hours in a standard working day, used to turn a day rate into an hourly rate. */
    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal standardHoursPerDay = new BigDecimal("8.00");

    // ── Salary component defaults ────────────────────────────
    // Percentages OF BASIC, used to pre-fill a new salary structure. They are
    // defaults, not rules: the per-employee SalaryStructure stores real amounts,
    // so a company can still pay one grade 40% house rent and another 20%.

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal houseRentPercent = new BigDecimal("40.00");

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal medicalPercent = new BigDecimal("10.00");

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal transportPercent = new BigDecimal("10.00");

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal foodPercent = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal providentFundPercent = new BigDecimal("10.00");

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercent = new BigDecimal("5.00");
}
