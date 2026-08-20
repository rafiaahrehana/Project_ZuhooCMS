package com.businessos.modules.finance.fixedasset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepreciationRunRepository extends JpaRepository<DepreciationRun, Long> {

    boolean existsByCompanyIdAndYearAndMonth(Long companyId, int year, int month);

    List<DepreciationRun> findByCompanyIdOrderByYearDescMonthDesc(Long companyId);

    Optional<DepreciationRun> findByIdAndCompanyId(Long id, Long companyId);
}
