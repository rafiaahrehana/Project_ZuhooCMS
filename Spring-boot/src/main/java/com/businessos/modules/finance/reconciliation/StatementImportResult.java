package com.businessos.modules.finance.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Outcome of a bank-statement CSV import against a reconciliation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImportResult {
    private int totalLines;
    private int matched;
    private int unmatchedCount;
    private List<UnmatchedLine> unmatchedLines;
    private BankReconciliationResponse reconciliation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedLine {
        private String date;
        private String description;
        private BigDecimal amount;
        private String reason;
    }
}
