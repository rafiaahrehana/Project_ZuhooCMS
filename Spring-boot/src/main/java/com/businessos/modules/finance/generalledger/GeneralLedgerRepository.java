package com.businessos.modules.finance.generalledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, Long> {

    Optional<GeneralLedger> findByIdAndCompanyId(Long id, Long companyId);

    Page<GeneralLedger> findByCompanyIdAndAccountId(Long companyId, Long accountId, Pageable pageable);

    Page<GeneralLedger> findByCompanyIdAndTransactionDateBetween(Long companyId, LocalDate start, LocalDate end, Pageable pageable);

    Page<GeneralLedger> findByCompanyId(Long companyId, Pageable pageable);

    List<GeneralLedger> findByCompanyIdAndReferenceTypeAndReferenceId(Long companyId, String type, Long id);

    boolean existsByAccountIdAndCompanyId(Long accountId, Long companyId);

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.companyId = :companyId AND gl.transactionDate BETWEEN :start AND :end")
    List<GeneralLedger> findTransactionsBetweenDates(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Used by FinancialReportServiceImpl.generateBalanceSheetReport to compute each
     * account type's balance as of a given date (not just its current live balance).
     */
    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.companyId = :companyId AND gl.account.id IN :accountIds AND gl.transactionDate <= :date")
    List<GeneralLedger> findByCompanyIdAndAccountIdsUpToDate(
        @Param("companyId") Long companyId, @Param("accountIds") List<Long> accountIds, @Param("date") LocalDate date);

    /**
     * Used by FinancialReportServiceImpl.generateAccountLedger to compute the account's
     * opening balance as of the ledger's start date (transactions strictly before it).
     */
    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.companyId = :companyId AND gl.account.id = :accountId AND gl.transactionDate < :date")
    List<GeneralLedger> findByCompanyIdAndAccountIdBeforeDate(
        @Param("companyId") Long companyId, @Param("accountId") Long accountId, @Param("date") LocalDate date);

    /**
     * Candidate "outstanding" lines for bank reconciliation: transactions posted to
     * this account, dated on or before the reconciliation's as-of date, that haven't
     * cleared the bank yet (isReconciled = false).
     */
    List<GeneralLedger> findByCompanyIdAndAccountIdAndIsReconciledFalseAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
        Long companyId, Long accountId, LocalDate asOfDate);

    /**
     * Used by year-end closing (AccountingPeriodServiceImpl) to compute one revenue/
     * expense account's movement for exactly the fiscal year being closed.
     */
    List<GeneralLedger> findByCompanyIdAndAccountIdAndTransactionDateBetween(
        Long companyId, Long accountId, LocalDate start, LocalDate end);
}