package com.businessos.modules.hrm.employee;

import com.businessos.enums.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserId(Long userId);

    /** Login user ids for a company. Employees with no user account are skipped. */
    @Query("SELECT e.user.id FROM Employee e WHERE e.company.id = :companyId AND e.user IS NOT NULL AND e.deleted = false")
    List<Long> findUserIdsByCompanyId(@Param("companyId") Long companyId);

    Optional<Employee> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Employee> findByCompanyIdAndEmployeeNumber(Long companyId, String employeeNumber);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    Page<Employee> findByCompanyId(Long companyId, Pageable pageable);

    List<Employee> findByCompanyIdAndActiveTrue(Long companyId);

    Page<Employee> findByCompanyIdAndDepartmentId(Long companyId, Long departmentId, Pageable pageable);

    /** True if this employee has at least one direct report - the definition of "manager" used for announcement audience targeting. */
    boolean existsByCompanyIdAndReportingManagerIdAndActiveTrue(Long companyId, Long reportingManagerId);

    /** This manager's own direct reports only - never the whole company - for the AI agent's generate_team_report tool. */
    List<Employee> findByCompanyIdAndReportingManagerIdAndActiveTrue(Long companyId, Long reportingManagerId);

    /** Active employees who ARE someone's reporting manager, for the MANAGERS announcement audience. */
    @Query("SELECT DISTINCT e FROM Employee e WHERE e.id IN "
         + "(SELECT DISTINCT r.reportingManager.id FROM Employee r WHERE r.company.id = :companyId AND r.reportingManager IS NOT NULL AND r.active = true) "
         + "AND e.company.id = :companyId AND e.active = true")
    Page<Employee> findManagersByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    /** Active employees who are NOT anyone's reporting manager, for the EMPLOYEES (individual contributor) announcement audience. */
    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.active = true AND e.id NOT IN "
         + "(SELECT DISTINCT r.reportingManager.id FROM Employee r WHERE r.company.id = :companyId AND r.reportingManager IS NOT NULL AND r.active = true)")
    Page<Employee> findNonManagersByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    // excludeOwner support - the company owner gets an auto-created Employee record
    // (so leave/timesheet/expense/payroll "my work" lookups don't 404 for them), but
    // the HRM Employees admin page shouldn't list the owner as a manageable employee.
    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "WHERE e.company.id = :companyId " +
           "AND (:excludedUserId IS NULL OR u.id IS NULL OR u.id != :excludedUserId) " +
           "AND e.deleted = false")
    Page<Employee> findByCompanyIdExcludingOwner(
            @Param("companyId") Long companyId,
            @Param("excludedUserId") Long excludedUserId,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "LEFT JOIN e.department d " +
           "WHERE e.company.id = :companyId " +
           "AND d.id = :departmentId " +
           "AND (:excludedUserId IS NULL OR u.id IS NULL OR u.id != :excludedUserId) " +
           "AND e.deleted = false")
    Page<Employee> findByCompanyIdAndDepartmentIdExcludingOwner(
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("excludedUserId") Long excludedUserId,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "WHERE e.company.id = :companyId " +
           "AND e.employmentStatus = :status " +
           "AND (:excludedUserId IS NULL OR u.id IS NULL OR u.id != :excludedUserId) " +
           "AND e.deleted = false")
    Page<Employee> findByCompanyIdAndEmploymentStatusExcludingOwner(
            @Param("companyId") Long companyId,
            @Param("status") EmploymentStatus status,
            @Param("excludedUserId") Long excludedUserId,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "LEFT JOIN e.department d " +
           "WHERE e.company.id = :companyId " +
           "AND d.id = :departmentId " +
           "AND e.employmentStatus = :status " +
           "AND (:excludedUserId IS NULL OR u.id IS NULL OR u.id != :excludedUserId) " +
           "AND e.deleted = false")
    Page<Employee> findByCompanyIdAndDepartmentIdAndEmploymentStatusExcludingOwner(
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("status") EmploymentStatus status,
            @Param("excludedUserId") Long excludedUserId,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "LEFT JOIN e.department d " +
           "LEFT JOIN e.designation des " +
           "WHERE e.company.id = :companyId " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND e.employmentStatus = :status " +
           "AND (:ownerUserId IS NULL OR u.id != :ownerUserId) " +
           "AND (:search IS NULL OR (" +
           "    LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    CAST(e.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(e.officialEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    CAST(d.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "    LOWER(des.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(des.code) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")) " +
           "AND e.deleted = false")
    Page<Employee> searchEmployeesWithStatus(
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("status") EmploymentStatus status,
            @Param("ownerUserId") Long ownerUserId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN e.user u " +
           "LEFT JOIN e.department d " +
           "LEFT JOIN e.designation des " +
           "WHERE e.company.id = :companyId " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND (:ownerUserId IS NULL OR u.id != :ownerUserId) " +
           "AND (:search IS NULL OR (" +
           "    LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    CAST(e.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(e.officialEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    CAST(d.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "    LOWER(des.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "    LOWER(des.code) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")) " +
           "AND e.deleted = false")
    Page<Employee> searchEmployeesWithoutStatus(
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("ownerUserId") Long ownerUserId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.company.id = :companyId AND e.deleted = false")
    long countByCompanyId(@Param("companyId") Long companyId);

    long countByCompanyIdAndUserIdNot(Long companyId, Long excludedUserId);

    // ── HR dashboard aggregations ─────────────────────────────
    // All active-only: a resigned employee should not appear in headcount,
    // department splits, birthday lists or probation alerts.

    long countByCompanyIdAndActiveTrueAndHireDateBetween(
        Long companyId, java.time.LocalDate from, java.time.LocalDate to);

    /** Headcount per department, largest first. Employees with no department are excluded. */
    @Query("""
        SELECT d.name, COUNT(e)
          FROM Employee e JOIN e.department d
         WHERE e.company.id = :companyId AND e.active = true AND e.deleted = false
         GROUP BY d.name
         ORDER BY COUNT(e) DESC
        """)
    java.util.List<Object[]> countByDepartment(@Param("companyId") Long companyId);

    /** Most recent joiners, newest first. */
    @Query("""
        SELECT e FROM Employee e
         WHERE e.company.id = :companyId AND e.active = true AND e.deleted = false
           AND e.hireDate IS NOT NULL
         ORDER BY e.hireDate DESC
        """)
    java.util.List<Employee> findRecentJoiners(@Param("companyId") Long companyId, Pageable pageable);

    /**
     * Birthdays falling in a day-of-year window. Compared on month/day rather
     * than the full date, since the year of birth is irrelevant here.
     */
    @Query("""
        SELECT e FROM Employee e
         WHERE e.company.id = :companyId AND e.active = true AND e.deleted = false
           AND e.dateOfBirth IS NOT NULL
           AND (FUNCTION('to_char', e.dateOfBirth, 'MM-DD') BETWEEN :fromMmDd AND :toMmDd)
         ORDER BY FUNCTION('to_char', e.dateOfBirth, 'MM-DD') ASC
        """)
    java.util.List<Employee> findBirthdaysBetween(
        @Param("companyId") Long companyId,
        @Param("fromMmDd") String fromMmDd,
        @Param("toMmDd") String toMmDd);

    /** Probation periods ending inside a window - HR needs to confirm or extend. */
    @Query("""
        SELECT e FROM Employee e
         WHERE e.company.id = :companyId AND e.active = true AND e.deleted = false
           AND e.probationEndDate BETWEEN :from AND :to
         ORDER BY e.probationEndDate ASC
        """)
    java.util.List<Employee> findProbationEndingBetween(
        @Param("companyId") Long companyId,
        @Param("from") java.time.LocalDate from,
        @Param("to") java.time.LocalDate to);

    /**
     * Cross-company (runs outside an HTTP request context - scheduler), matching
     * the convention used by LicenseExpiryScheduler. Unlike probationEndDate,
     * contractEndDate passing had no alert of any kind - payroll kept paying
     * past it with nothing flagging the contract had lapsed.
     */
    @Query("""
        SELECT e FROM Employee e
         WHERE e.active = true AND e.deleted = false
           AND e.contractEndDate BETWEEN :from AND :to
           AND e.contractEndReminderSentAt IS NULL
        """)
    java.util.List<Employee> findContractEndingSoonUnnotified(
        @Param("from") java.time.LocalDate from,
        @Param("to") java.time.LocalDate to);

    /**
     * Headcount as at a date: hired on or before it, and not since deactivated.
     * Drives the month-to-date headcount trend. This is a reconstruction from
     * hire dates, not a stored daily snapshot - see HrDashboardResponse.
     */
    @Query("""
        SELECT COUNT(e) FROM Employee e
         WHERE e.company.id = :companyId AND e.deleted = false
           AND e.hireDate IS NOT NULL AND e.hireDate <= :asOf
        """)
    long countHiredOnOrBefore(@Param("companyId") Long companyId, @Param("asOf") java.time.LocalDate asOf);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.department.name = :departmentName AND e.active = true")
    List<Employee> findByDepartment(@Param("companyId") Long companyId, @Param("departmentName") String departmentName);

    /**
     * Resolves the company ID for a platformuser who is an employee.
     * Used by AuthServiceImpl.resolveCompanyId() in Phase 3+.
     */
    @Query("SELECT e.company.id FROM Employee e WHERE e.user.id = :userId AND e.active = true AND e.deleted = false")
    Optional<Long> findCompanyIdByUserId(Long userId);
    boolean existsByCompanyId(Long companyId);

    long countByDesignationId(Long designationId);
    long countByShiftId(Long shiftId);

    /**
     * Used by EmployeeNumberGenerator - MAX-based (not COUNT) to be safe against
     * concurrent inserts and deleted records skewing the sequence.
     */
    @Query("SELECT MAX(e.employeeNumber) FROM Employee e WHERE e.company.id = :companyId AND e.employeeNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxEmployeeNumberByCompanyAndPrefix(@Param("companyId") Long companyId, @Param("prefix") String prefix);

    /**
     * Used by EmployeeNumberBackfillInitializer to find pre-existing employees
     * that predate auto-generation and never had a number set.
     */
    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND (e.employeeNumber IS NULL OR e.employeeNumber = '') ORDER BY e.id ASC")
    List<Employee> findByCompanyIdWithBlankEmployeeNumber(@Param("companyId") Long companyId);
}
