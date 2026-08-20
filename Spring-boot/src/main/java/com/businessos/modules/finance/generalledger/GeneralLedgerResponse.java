package com.businessos.modules.finance.generalledger;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GeneralLedgerResponse {
    private Long id;
    private Long companyId;
    private LocalDate transactionDate;
    private Long accountId;
    private String accountName;
    private String accountCode;
    private com.businessos.modules.finance.chartofaccounts.AccountType accountType;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String description;
    private String referenceType;
    private Long referenceId;
    private String referenceNumber;
    // Jackson strips the "is" prefix from Lombok's isReconciled() getter by default
    // (JSON key would be "reconciled"), which wouldn't match the frontend's
    // isReconciled field - force the full name explicitly.
    @JsonProperty("isReconciled")
    private boolean isReconciled;
    private String reconciliationNotes;
    private String postedBy;
    private LocalDate postedDate;
    private boolean posted;
}
