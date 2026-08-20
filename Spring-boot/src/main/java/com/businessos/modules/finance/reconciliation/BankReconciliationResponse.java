package com.businessos.modules.finance.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationResponse {
    private Long id;
    private Long companyId;
    
    private Long bankAccountId;
    private String bankAccountName;
    
    private LocalDate reconciliationDate;
    
    private BigDecimal glBalance;
    private BigDecimal bankStatementBalance;
    private BigDecimal difference;

    private BigDecimal outstandingDepositsTotal;
    private BigDecimal outstandingChecksTotal;
    // bankStatementBalance + outstandingDepositsTotal - outstandingChecksTotal - a
    // convenience so the UI doesn't have to recompute it; should equal glBalance
    // (difference == 0) before the reconciliation can be closed.
    private BigDecimal adjustedBankBalance;

    private boolean reconciled;
    private LocalDate reconciledDate;
    private String reconciledBy;
    private String discrepancyNotes;

    private String statementFileName;
    private String statementFileUrl;
    private java.time.LocalDateTime statementUploadedAt;
}
