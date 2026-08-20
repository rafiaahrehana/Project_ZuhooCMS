package com.businessos.modules.finance.reports.dto;

import com.businessos.modules.finance.generalledger.GeneralLedgerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLedger {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<GeneralLedgerResponse> entries;
    private LocalDate generatedDate;
}
