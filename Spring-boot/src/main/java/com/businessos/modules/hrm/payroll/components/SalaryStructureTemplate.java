package com.businessos.modules.hrm.payroll.components;

import com.businessos.core.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;

/**
 * Reusable salary structure (the spec's salary_structure table): a named
 * recipe like "Software Engineer Grade A" - basic as a % of gross, HRA as a
 * % of basic, fixed allowance amounts, and whatever is left lands in special
 * allowance. Applying a template to an employee stamps the numbers onto that
 * employee's own SalaryStructure row; the template stays untouched.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "salary_structure_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryStructureTemplate extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String structureName;

    /**
     * The grade's standard package (e.g. "Software Engineer Grade A" pays
     * 100,000). Selecting the template on an empty form fills Gross from
     * here, then everything else derives - a grade IS the whole package.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal defaultGross;

    /** % of gross. */
    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal basicPercentage = new BigDecimal("50");

    /** % of basic, per the spec. */
    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal hraPercentage = new BigDecimal("40");

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal medicalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal transportAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal internetAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal mobileAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal mealAmount = BigDecimal.ZERO;

    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
