package com.businessos.modules.finance.period;

import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {

    /** The 12 periods for this fiscal year, generating any that don't exist yet. */
    List<AccountingPeriodResponse> listForYear(int fiscalYear);

    /** Rollup of every fiscal year that has periods - the Fiscal Years overview page. */
    List<FiscalYearSummary> listFiscalYears();

    AccountingPeriodResponse closePeriod(Long id);

    AccountingPeriodResponse reopenPeriod(Long id);

    /** Requires all 12 periods of the year to already be closed. Idempotent. */
    void closeFiscalYear(int fiscalYear);

    /**
     * True if `date` falls inside a period that's already CLOSED for this company.
     * Used by GeneralLedgerServiceImpl.recordTransaction() to block backdated postings.
     * A date with no period ever created for it is never considered closed.
     */
    boolean isDateInClosedPeriod(Long companyId, LocalDate date);
}
