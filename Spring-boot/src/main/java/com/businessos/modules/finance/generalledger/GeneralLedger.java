package com.businessos.modules.finance.generalledger;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "general_ledger", indexes = {
        @Index(name = "idx_gl_account", columnList = "chart_of_account_id"),
        @Index(name = "idx_gl_date", columnList = "transaction_date"),
        @Index(name = "idx_gl_reference", columnList = "reference_type, reference_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneralLedger extends BaseEntity {

    private Long companyId; // Tenant isolation

    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chart_of_account_id", nullable = false)
    private ChartOfAccount account;

    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    private String description; // e.g., "Payment for invoice #INV-001"

    // Reference to source transaction
    private String referenceType; // EXPENSE, INVOICE, SALARY, VENDOR_PAYMENT, JOURNAL_ENTRY
    private Long referenceId; // FK to source table

    private String referenceNumber; // e.g., "INV-001", "EXP-005"

    @Builder.Default
    private boolean isReconciled = false; // For bank reconciliation

    private String reconciliationNotes;

    // Which BankReconciliation cleared this line, if any - lets a reconciliation's
    // "still outstanding" set be computed live (unreconciled entries for the account)
    // instead of the user hand-typing amounts. Plain id (not a @ManyToOne) to avoid a
    // cross-package dependency on the reconciliation module, same pattern as referenceId.
    private Long reconciledInReconciliationId;

    private String postedBy; // User who posted this GL entry
    private LocalDate postedDate;

    @Builder.Default
    private boolean posted = true; // GL entry is posted

    public BigDecimal getAmount() {
        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            return debitAmount;
        }
        return creditAmount;
    }

    public String getType() {
        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "DEBIT";
        }
        return "CREDIT";
    }
}