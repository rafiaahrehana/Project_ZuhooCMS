package com.businessos.modules.finance.expense;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity(name = "FinanceExpense")
@Table(name = "expenses", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "expense_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "expense_number", nullable = false)
    private String expenseNumber; // EXP-2024-001

    private String title;

    @Builder.Default
    private String currency = "BDT";

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee submittedBy;

    private String description;
    private BigDecimal amount;

    private String vendorName; //to whom payment

    private String category; // TRAVEL, MEALS, OFFICE_SUPPLIES, LICENSING, etc.

    // The COA expense account this posts to when paid. Optional - falls back to the
    // generic Operating Expenses account, so the category label alone no longer
    // decides (or rather, fails to decide) where the money lands in the P&L.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_id")
    private com.businessos.modules.finance.chartofaccounts.ChartOfAccount expenseAccount;

    private LocalDate expenseDate;
    private String receiptUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDate approvedDate;
    private String approvalNotes;

    private LocalDate reimbursedDate;
    private String reimbursementMethod;
    private String referenceNumber; // Bank tx id, wire id

    private String notes;

    public void approve(User approver) {
        this.approvedBy = approver;
        this.approvedDate = LocalDate.now();
        this.status = ExpenseStatus.APPROVED;
    }

    public void reject() {
        this.status = ExpenseStatus.REJECTED;
    }

    public void markAsPaid(String reimbursementMethod, String referenceNumber) {
        this.status = ExpenseStatus.PAID;
        this.reimbursedDate = LocalDate.now();
        this.reimbursementMethod = reimbursementMethod;
        this.referenceNumber = referenceNumber;
    }
}
