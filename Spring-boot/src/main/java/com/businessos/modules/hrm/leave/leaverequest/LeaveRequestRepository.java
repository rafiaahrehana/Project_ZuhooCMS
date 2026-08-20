package com.businessos.modules.hrm.leave.leaverequest;

import com.businessos.enums.LeaveRequestStatus;
import com.businessos.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByIdAndCompanyId(Long id, Long companyId);

    Page<LeaveRequest> findByCompanyId(Long companyId, Pageable pageable);

    Page<LeaveRequest> findByCompanyIdAndStatus(
        Long companyId, LeaveRequestStatus status, Pageable pageable);

    long countByCompanyIdAndStatus(Long companyId, LeaveRequestStatus status);

    /**
     * Requests whose leave falls inside a window, by status - the HR dashboard's
     * monthly leave summary. Counted on the leave dates rather than when the
     * request was raised, so "this month" means leave taken this month.
     */
    long countByCompanyIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long companyId, LeaveRequestStatus status,
        java.time.LocalDate to, java.time.LocalDate from);

    long countByCompanyIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long companyId, java.time.LocalDate to, java.time.LocalDate from);

    Page<LeaveRequest> findByCompanyIdAndEmployeeId(
        Long companyId, Long employeeId, Pageable pageable);

    /**
     * Approved leave days an employee took inside a window — the leaves-taken KPI.
     * COALESCE keeps the return 0 rather than null when there is no leave at all.
     */
    @Query("""
        SELECT COALESCE(SUM(lr.totalDays), 0) FROM LeaveRequest lr
        WHERE lr.company.id = :companyId
          AND lr.employee.id = :employeeId
          AND lr.status = com.businessos.enums.LeaveRequestStatus.APPROVED
          AND lr.startDate <= :to AND lr.endDate >= :from
          AND lr.deleted = false
        """)
    Integer sumApprovedLeaveDaysInRange(
        @Param("companyId") Long companyId,
        @Param("employeeId") Long employeeId,
        @Param("from") java.time.LocalDate from,
        @Param("to") java.time.LocalDate to);

    @Query("""
        SELECT SUM(lr.totalDays) FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.leaveType = :leaveType
          AND lr.status = :status
          AND YEAR(lr.startDate) = :year
          AND lr.deleted = false
        """)
    Optional<Integer> sumDaysByEmployeeAndTypeAndStatusAndYear(
        @Param("employeeId") Long employeeId,
        @Param("leaveType") LeaveType leaveType,
        @Param("status") LeaveRequestStatus status,
        @Param("year") int year);

    @Query("""
        SELECT COUNT(lr) > 0 FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status NOT IN :excludedStatuses
          AND lr.startDate <= :endDate
          AND lr.endDate >= :startDate
          AND lr.deleted = false
        """)
    boolean hasOverlappingLeave(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludedStatuses") java.util.List<LeaveRequestStatus> excludedStatuses);

    /**
     * Used by DailyAbsenteeScheduler to skip employees who are on approved leave
     * for the day, rather than marking them ABSENT.
     */
    @Query("""
        SELECT COUNT(lr) > 0 FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status = :status
          AND lr.startDate <= :date
          AND lr.endDate >= :date
          AND lr.deleted = false
        """)
    boolean existsApprovedForEmployeeAndDate(
        @Param("employeeId") Long employeeId,
        @Param("date") LocalDate date,
        @Param("status") LeaveRequestStatus status);

    /**
     * Used by the employee dashboard's monthly summary - approved leave requests
     * that overlap a given date range (e.g. the current month), so per-day overlap
     * can be clamped/counted in Java.
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status = :status
          AND lr.startDate <= :rangeEnd
          AND lr.endDate >= :rangeStart
          AND lr.deleted = false
        """)
    List<LeaveRequest> findApprovedOverlapping(
        @Param("employeeId") Long employeeId,
        @Param("status") LeaveRequestStatus status,
        @Param("rangeStart") LocalDate rangeStart,
        @Param("rangeEnd") LocalDate rangeEnd);

    /** Approved requests of one type overlapping [from, to]. */
    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status = com.businessos.enums.LeaveRequestStatus.APPROVED
          AND lr.leaveType = :leaveType
          AND lr.startDate <= :to
          AND lr.endDate >= :from
          AND lr.deleted = false
        """)
    java.util.List<LeaveRequest> findApprovedOverlapping(
        @Param("employeeId") Long employeeId,
        @Param("leaveType") com.businessos.enums.LeaveType leaveType,
        @Param("from") java.time.LocalDate from,
        @Param("to") java.time.LocalDate to);
}
