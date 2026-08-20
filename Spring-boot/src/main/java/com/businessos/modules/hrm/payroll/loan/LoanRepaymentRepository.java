package com.businessos.modules.hrm.payroll.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoanIdOrderByPaidDateDesc(Long loanId);
}
