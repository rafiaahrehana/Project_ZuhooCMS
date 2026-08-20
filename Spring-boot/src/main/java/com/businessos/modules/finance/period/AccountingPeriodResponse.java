package com.businessos.modules.finance.period;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPeriodResponse {
    private Long id;
    private int fiscalYear;
    private int periodNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String label; // e.g. "Jan 2026" - the actual calendar month/year this period covers
    private PeriodStatus status;
    private String closedBy;
    private LocalDateTime closedAt;
    private String reopenedBy;
    private LocalDateTime reopenedAt;
}
