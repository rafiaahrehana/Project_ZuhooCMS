package com.businessos.modules.finance.reconciliation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    Optional<BankReconciliation> findByIdAndCompanyId(Long id, Long companyId);

    Page<BankReconciliation> findByCompanyId(Long companyId, Pageable pageable);

    List<BankReconciliation> findByCompanyIdAndReconciledFalse(Long companyId);

    /**
     * Used by BankReconciliationOverdueScheduler - matches on the exact day a pending
     * reconciliation crosses the overdue threshold, so it's flagged once rather than
     * every day it stays pending (no extra "already notified" column needed).
     */
    List<BankReconciliation> findByCompanyIdAndReconciledFalseAndReconciliationDate(Long companyId, LocalDate reconciliationDate);
}
