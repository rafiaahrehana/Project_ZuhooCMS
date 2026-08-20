package com.businessos.modules.finance.reconciliation;

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
@Table(name = "bank_reconciliations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankReconciliation extends BaseEntity {

    private Long companyId; // Tenant isolation

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private ChartOfAccount bankAccount;

    private LocalDate reconciliationDate;

    private BigDecimal glBalance;
    private BigDecimal bankStatementBalance;

    // glBalance - adjustedBankBalance, where adjustedBankBalance = bankStatementBalance
    // + outstandingDepositsTotal - outstandingChecksTotal. Must be (near) zero before
    // this can be marked reconciled - see BankReconciliationServiceImpl.markAsReconciled.
    private BigDecimal difference;

    // Computed live from GeneralLedger entries for this account that are still
    // isReconciled=false as of reconciliationDate - not hand-typed.
    @Builder.Default
    private BigDecimal outstandingDepositsTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal outstandingChecksTotal = BigDecimal.ZERO;

    @Builder.Default
    private boolean reconciled = false;

    private String discrepancyNotes;
    private LocalDate reconciledDate;
    private String reconciledBy;

    // The actual bank statement (PDF/image) this reconciliation was matched against -
    // audit trail for "what did the bank say at the time," not derivable from the GL.
    private String statementFileName;
    private String statementFileUrl;
    private java.time.LocalDateTime statementUploadedAt;

    public void markAsReconciled(String reconciledByName) {
        this.reconciled = true;
        this.reconciledDate = LocalDate.now();
        this.reconciledBy = reconciledByName;
    }

    public boolean hasDiscrepancy() {
        return difference != null && difference.compareTo(BigDecimal.ZERO) != 0;
    }
}