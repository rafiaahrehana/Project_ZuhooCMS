package com.businessos.modules.finance.chartofaccounts;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "chart_of_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "account_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccount extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(nullable = false)
    private String accountCode; // e.g., "1000", "5100"

    private String accountName; // e.g., "Cash", "Salary Expense"

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private AccountType type; // ASSET, LIABILITY, REVENUE, EXPENSE, etc.

    private String description; // Detailed explanation

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO; // Current balance

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean isHeaderAccount = false; // Parent account for grouping

    // Marks this as a real bank/cash account, so Bank Reconciliation's account picker
    // can show only accounts that make sense to reconcile against a bank statement -
    // without this, any ASSET-type account (Fixed Assets, Accounts Receivable, an
    // equipment account, etc.) looks identical to an actual bank account.
    @Builder.Default
    private boolean isBankAccount = false;

    // For sub-accounts
    private Long parentAccountId; // If this is a sub-account

    // Configuration
    @Builder.Default
    private boolean allowDirectPosting = true; // Can JE be posted directly?

    private String notes;

    public BigDecimal getDebitBalance() {
        return type.isCreditNormal() ? BigDecimal.ZERO : balance;
    }

    public BigDecimal getCreditBalance() {
        return type.isCreditNormal() ? balance : BigDecimal.ZERO;
    }
}