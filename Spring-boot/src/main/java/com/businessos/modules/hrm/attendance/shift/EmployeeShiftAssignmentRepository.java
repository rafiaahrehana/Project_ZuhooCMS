package com.businessos.modules.hrm.attendance.shift;

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
public interface EmployeeShiftAssignmentRepository extends JpaRepository<EmployeeShiftAssignment, Long> {

    Optional<EmployeeShiftAssignment> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * The employee's shift as of a given date.
     *
     * The date window is part of the condition, not just the `active` flag:
     * `active` is set by hand and nobody goes back to clear it, so assignments
     * whose end date passed years ago are still flagged active in real data.
     * Trusting the flag alone made a long-expired assignment look like the
     * employee's current shift, which then drove late detection.
     *
     * Ordered newest-first and returned as a list because an employee can have
     * several rows whose windows overlap; the caller takes the most recent
     * rather than the query blowing up on a non-unique result.
     */
    @Query("""
            SELECT esa FROM EmployeeShiftAssignment esa
            WHERE esa.companyId = :companyId
              AND esa.employee.id = :employeeId
              AND esa.active = true
              AND esa.deleted = false
              AND (esa.assignmentStartDate IS NULL OR esa.assignmentStartDate <= :onDate)
              AND (esa.assignmentEndDate IS NULL OR esa.assignmentEndDate >= :onDate)
            ORDER BY esa.assignmentStartDate DESC, esa.id DESC
            """)
    List<EmployeeShiftAssignment> findEffectiveAssignments(@Param("companyId") Long companyId,
                                                           @Param("employeeId") Long employeeId,
                                                           @Param("onDate") LocalDate onDate);

    /** The shift in force for this employee today, if any. */
    default Optional<EmployeeShiftAssignment> findByCompanyIdAndEmployeeIdAndActive(Long companyId, Long employeeId) {
        return findEffectiveAssignments(companyId, employeeId, LocalDate.now())
                .stream()
                .findFirst();
    }

    Page<EmployeeShiftAssignment> findByCompanyIdAndShiftIdAndActiveTrue(Long companyId, Long shiftId, Pageable pageable);

    Page<EmployeeShiftAssignment> findByCompanyId(Long companyId, Pageable pageable);
}
