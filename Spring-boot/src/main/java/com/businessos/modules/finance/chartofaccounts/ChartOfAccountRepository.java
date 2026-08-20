package com.businessos.modules.finance.chartofaccounts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {

    Optional<ChartOfAccount> findByCompanyIdAndAccountCode(Long companyId, String accountCode);

    Optional<ChartOfAccount> findByIdAndCompanyId(Long id, Long companyId);

    Page<ChartOfAccount> findByCompanyIdAndTypeAndActiveTrue(Long companyId, AccountType type, Pageable pageable);

    Page<ChartOfAccount> findByCompanyIdAndActive(Long companyId, boolean active, Pageable pageable);

    Page<ChartOfAccount> findByCompanyId(Long companyId, Pageable pageable);

    List<ChartOfAccount> findByCompanyIdAndType(Long companyId, AccountType type);

    List<ChartOfAccount> findByCompanyIdAndActive(Long companyId, boolean active);
}
