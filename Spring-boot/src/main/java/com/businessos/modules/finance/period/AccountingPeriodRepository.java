package com.businessos.modules.finance.period;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findByCompanyIdAndFiscalYearAndPeriodNumber(Long companyId, int fiscalYear, int periodNumber);

    List<AccountingPeriod> findByCompanyIdAndFiscalYearOrderByPeriodNumberAsc(Long companyId, int fiscalYear);

    Optional<AccountingPeriod> findByIdAndCompanyId(Long id, Long companyId);

    /** The period whose date range contains the given date, if it's ever been created. */
    Optional<AccountingPeriod> findByCompanyIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long companyId, LocalDate date1, LocalDate date2);

    List<AccountingPeriod> findByCompanyIdOrderByFiscalYearAscPeriodNumberAsc(Long companyId);
}
