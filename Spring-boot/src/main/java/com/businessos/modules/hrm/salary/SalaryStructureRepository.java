package com.businessos.modules.hrm.salary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    Optional<SalaryStructure> findByIdAndCompanyId(Long id, Long companyId);

    Page<SalaryStructure> findByCompanyIdAndEmployeeId(Long companyId, Long employeeId, Pageable pageable);

    /**
     * Every structure in the company, newest first — the unfiltered list view.
     * The fetch joins matter here: SalaryStructureMapper reads employee.user.fullName
     * and approvedBy.fullName, both LAZY, so without them a page of N rows costs
     * 2N+1 queries. These are all ManyToOne (single-valued) associations, so
     * Hibernate can still paginate in SQL rather than in memory.
     * Soft-deleted rows are excluded automatically by @SQLRestriction on BaseEntity.
     */
    @Query(value = """
        SELECT s FROM SalaryStructure s
        LEFT JOIN FETCH s.employee e
        LEFT JOIN FETCH e.user
        LEFT JOIN FETCH s.approvedBy
        WHERE s.company.id = :companyId
        """,
        countQuery = "SELECT COUNT(s) FROM SalaryStructure s WHERE s.company.id = :companyId")
    Page<SalaryStructure> findAllInCompany(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * Returns the currently active structure — effectiveTo IS NULL.
     */
    Optional<SalaryStructure> findByEmployeeIdAndEffectiveToIsNull(Long employeeId);

    @Query("""
        SELECT s FROM SalaryStructure s
        WHERE s.employee.id = :employeeId
          AND s.effectiveFrom <= :date
          AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)
          AND s.deleted = false
        """)
    Optional<SalaryStructure> findActiveForEmployeeOnDate(
        @Param("employeeId") Long employeeId,
        @Param("date") LocalDate date);

    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
}
