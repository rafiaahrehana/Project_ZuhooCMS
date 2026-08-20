package com.businessos.modules.hrm.payroll.components;

import com.businessos.core.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Company-scoped catalog of salary components (the spec's salary_component
 * table). Earnings, deductions and employer contributions each company can
 * attach to an employee's salary structure. A standard IT-company catalog is
 * seeded on first read; companies can add, rename or deactivate entries.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "salary_components")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryComponent extends BaseEntity {

    public enum ComponentType { EARNING, DEDUCTION, EMPLOYER_CONTRIBUTION }
    public enum CalculationType { FIXED, PERCENTAGE }

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CalculationType calculationType = CalculationType.FIXED;

    @Builder.Default
    private Boolean taxable = Boolean.TRUE;

    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    private Integer sortOrder = 0;
}
