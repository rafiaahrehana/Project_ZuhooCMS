package com.businessos.modules.finance.period;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Thin, dependency-light check used by GeneralLedgerServiceImpl to reject posting into a
 * closed accounting period. Deliberately depends only on the repository (not
 * AccountingPeriodService) - AccountingPeriodServiceImpl itself calls into
 * GeneralLedgerService to post year-end closing entries, so routing this check through
 * the full service would create a circular bean dependency.
 */
@Component
@RequiredArgsConstructor
public class PeriodLockChecker {

    private final AccountingPeriodRepository periodRepository;

    public boolean isDateInClosedPeriod(Long companyId, LocalDate date) {
        if (companyId == null || date == null) return false;
        return periodRepository.findByCompanyIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(companyId, date, date)
                .map(p -> p.getStatus() == PeriodStatus.CLOSED)
                .orElse(false);
    }
}
