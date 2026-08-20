package com.businessos.modules.finance.reports.dto;

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
public class TrialBalanceReport {
    private LocalDate asOfDate;
    private List<AccountBalance> accounts;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private LocalDate generatedDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalance {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
    }
}
