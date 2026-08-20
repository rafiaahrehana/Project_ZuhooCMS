package com.businessos.modules.finance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Accounts Receivable aging as of a given date - every outstanding client invoice
 * bucketed by how overdue it is, so a company can see who owes them money and how
 * late it is (the report every business needs for AR follow-up).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeingReport {
    private LocalDate asOfDate;

    private BigDecimal current;     // not yet due
    private BigDecimal days1to30;
    private BigDecimal days31to60;
    private BigDecimal days61to90;
    private BigDecimal over90;
    private BigDecimal totalOutstanding;

    private List<AgeingLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgeingLine {
        private Long invoiceId;
        private String invoiceNumber;
        private Long clientId;
        private String clientName;
        private LocalDate dueDate;
        private BigDecimal balanceAmount;
        private long daysOverdue; // 0 or negative if not yet due
        private String bucket;    // CURRENT, 1-30, 31-60, 61-90, 90+
    }
}
