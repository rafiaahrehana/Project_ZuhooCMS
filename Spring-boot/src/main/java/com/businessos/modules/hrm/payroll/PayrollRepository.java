package com.businessos.modules.hrm.payroll;

import com.businessos.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    Optional<Payroll> findByIdAndCompanyId(Long id, Long companyId);

    java.util.List<Payroll> findAllByCompanyIdAndPayMonthAndPayYear(Long companyId, int payMonth, int payYear);

    java.util.List<Payroll> findByRunId(Long runId);

    Optional<Payroll> findByEmployeeIdAndPayMonthAndPayYear(
        Long employeeId, int payMonth, int payYear);

    Page<Payroll> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * All payrolls for a period in one status, employee (and their bank details)
     * fetched eagerly - the bank disbursement export reads those per row and
     * would otherwise fire a query per employee.
     */
    @Query("""
        SELECT p FROM Payroll p
        LEFT JOIN FETCH p.employee e
        LEFT JOIN FETCH e.user
        WHERE p.company.id = :companyId
          AND p.payMonth = :month AND p.payYear = :year
          AND p.status = :status
        ORDER BY e.employeeNumber ASC
        """)
    List<Payroll> findForDisbursement(
        @Param("companyId") Long companyId,
        @Param("month") int month,
        @Param("year") int year,
        @Param("status") PayrollStatus status);

    Page<Payroll> findByCompanyIdAndPayMonthAndPayYear(
        Long companyId, int payMonth, int payYear, Pageable pageable);

    long countByCompanyIdAndPayMonthAndPayYearAndStatusIn(
        Long companyId, int payMonth, int payYear, java.util.List<PayrollStatus> statuses);

    Page<Payroll> findByCompanyIdAndEmployeeId(
        Long companyId, Long employeeId, Pageable pageable);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.company.id = :companyId AND p.payMonth = :month AND p.payYear = :year AND p.status = :status AND p.deleted = false")
    Optional<BigDecimal> sumNetSalaryByCompanyAndPeriod(
        @Param("companyId") Long companyId,
        @Param("month") int month,
        @Param("year") int year,
        @Param("status") PayrollStatus status);
}
