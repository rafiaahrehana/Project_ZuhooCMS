package com.businessos.modules.hrm.payroll.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanAdvanceRepository extends JpaRepository<LoanAdvance, Long> {

    Optional<LoanAdvance> findByIdAndCompanyId(Long id, Long companyId);

    List<LoanAdvance> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<LoanAdvance> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    Optional<LoanAdvance> findFirstByEmployeeIdAndStatus(Long employeeId, LoanAdvance.Status status);

    boolean existsByEmployeeIdAndStatus(Long employeeId, LoanAdvance.Status status);
}
