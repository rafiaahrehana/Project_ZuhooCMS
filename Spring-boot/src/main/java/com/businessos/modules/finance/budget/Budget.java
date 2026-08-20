package com.businessos.modules.finance.budget;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;

/**
 * A spending target for one expense category in one fiscal year. Actual spend is
 * computed live from approved/paid expenses in that category - see BudgetService.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "category", "fiscal_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(nullable = false)
    private String category; // Matches Expense.category (free text, case-insensitive)

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(nullable = false)
    private BigDecimal amount;

    private String notes;
}
