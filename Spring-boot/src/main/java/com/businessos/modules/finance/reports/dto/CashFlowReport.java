package com.businessos.modules.finance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Direct-method cash flow for a period: where cash actually moved, grouped by
 * source (customer payments, expenses, payroll, other ledger activity), tying the
 * Cash account's opening balance to its closing balance for the period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowReport {
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalInflows;
    private BigDecimal totalOutflows;
    private BigDecimal netChange;

    private List<CashFlowLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowLine {
        private String category;
        private BigDecimal inflow;
        private BigDecimal outflow;
    }
}
