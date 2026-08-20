package com.businessos.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of the platform dashboard's trend charts - see PlatformMetricsSnapshot. */
@Getter
@AllArgsConstructor
public class PlatformMetricsPoint {
    private LocalDate date;
    private long totalCompanies;
    private long activeCompanies;
    private long trialCompanies;
    private long suspendedCompanies;
    private BigDecimal revenue;
}
