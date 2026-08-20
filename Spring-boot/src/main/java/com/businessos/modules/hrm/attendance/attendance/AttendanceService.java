package com.businessos.modules.hrm.attendance.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse checkIn(AttendanceCheckInRequest request);

    AttendanceResponse checkOut(Long attendanceId, AttendanceCheckOutRequest request);

    AttendanceResponse getMyTodayAttendance();

    /** The calling user's own attendance history - resolves their Employee record internally. */
    Page<AttendanceResponse> getMyRecords(Pageable pageable);

    /** The calling user's own Present/Absent/Half Day/On Leave/Holiday/Week Off/Worked Hours breakdown for a month. */
    MyAttendanceMonthlySummaryResponse getMyMonthlySummary(int year, int month);

    // ── Admin / HR actions ───────────────────────────────────────────────────
    /** Manual entry: create an attendance record for any date (admin / HR only). */
    AttendanceResponse createManual(AttendanceRequest request);

    // ── Reads ────────────────────────────────────────────────────────────────
    AttendanceResponse getById(Long id);

    AttendanceResponse getByEmployeeAndDate(Long employeeId, LocalDate date);

    Page<AttendanceResponse> getByEmployee(Long employeeId, Pageable pageable);

    Page<AttendanceResponse> getByCompanyAndDateRange(LocalDate start, LocalDate end, Pageable pageable);

    Page<AttendanceResponse> getByStatus(AttendanceStatus status, Pageable pageable);

    Page<AttendanceResponse> listAll(AttendanceStatus status, LocalDate date, LocalDate startDate, LocalDate endDate, String search, Pageable pageable);

    default Page<AttendanceResponse> listAll(Pageable pageable) {
        return listAll(null, null, null, null, null, pageable);
    }

    List<AttendanceResponse> getLateAttendances(LocalDate date);

    List<AttendanceResponse> getAbsentees(LocalDate date);

    // ── Dashboard counts ─────────────────────────────────────────────────────
    long countPresent(Long companyId, LocalDate date);

    long countLate(Long companyId, LocalDate date);

    long countAbsent(Long companyId, LocalDate date);

    // ── Mutations ────────────────────────────────────────────────────────────
    void updateStatus(Long id, AttendanceStatus status);

    void approveAttendance(Long id, String approverName);

    AttendanceResponse delete(Long id);
}
