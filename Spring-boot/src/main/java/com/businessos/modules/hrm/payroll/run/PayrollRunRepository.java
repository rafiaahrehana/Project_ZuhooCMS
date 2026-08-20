package com.businessos.modules.hrm.payroll.run;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByCompanyIdAndPayMonthAndPayYear(Long companyId, int month, int year);
    List<PayrollRun> findByCompanyIdOrderByPayYearDescPayMonthDesc(Long companyId);
    long countByCompanyId(Long companyId);
}
