package com.businessos.modules.hrm.payroll.components;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.salary.SalaryStructure;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * The spec's employee_salary_component: a catalog component attached to one
 * employee's salary structure with a concrete amount - internet allowance,
 * a loan EMI, health insurance, and so on. Earnings raise the payroll's
 * other-earnings total; deductions raise other-deductions. Employer
 * contributions are informational (they never touch net pay).
 */
@Entity
@Table(name = "employee_salary_components")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StructureExtraComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure structure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_component_id", nullable = false)
    private SalaryComponent component;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
