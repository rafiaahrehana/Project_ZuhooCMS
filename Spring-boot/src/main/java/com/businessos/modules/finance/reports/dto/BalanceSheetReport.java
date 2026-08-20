package com.businessos.modules.finance.reports.dto;

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
public class BalanceSheetReport {
    private LocalDate asOfDate;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    // Assets should equal Liabilities + Equity - if not, either an unbalanced entry was
    // posted somewhere or a fiscal year hasn't been closed yet (net income not yet rolled
    // into Retained Earnings). See AccountingPeriodService.closeFiscalYear.
    private boolean balanced;
    private BigDecimal outOfBalanceAmount;
    private LocalDate generatedDate;
}
