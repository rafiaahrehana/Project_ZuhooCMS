package com.businessos.modules.finance.period;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the Fiscal Years overview page - a rollup of a year's 12 AccountingPeriods.
 * Status: CLOSED once the year-end closing entry has posted; ACTIVE while the year
 * contains today or has any closed period (work has started); DRAFT for a generated
 * future year nobody has touched yet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalYearSummary {
    private int fiscalYear;
    private String name; // "FY 2026"
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalPeriods;
    private int openPeriods;
    private int closedPeriods;
    private String status; // DRAFT | ACTIVE | CLOSED
    private boolean yearEndPosted;
    private boolean current; // contains today
    private LocalDateTime createdAt;
}
