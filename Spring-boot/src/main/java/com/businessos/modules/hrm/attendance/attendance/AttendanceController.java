package com.businessos.modules.hrm.attendance.attendance;

import com.businessos.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AbsenteeMarkingService absenteeMarkingService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<AttendanceResponse>> listAll(
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.listAll(
                status, date, startDate, endDate, search,
                PageRequest.of(page, size, Sort.by("attendanceDate").descending())));
    }

    @GetMapping("/my/today")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> getMyTodayAttendance() {
        return ResponseEntity.ok(attendanceService.getMyTodayAttendance());
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<AttendanceResponse>> getMyRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getMyRecords(
                PageRequest.of(page, size, Sort.by("attendanceDate").descending())));
    }

    @GetMapping("/my/monthly-summary")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<MyAttendanceMonthlySummaryResponse> getMyMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int resolvedYear = year != null ? year : now.getYear();
        int resolvedMonth = month != null ? month : now.getMonthValue();
        return ResponseEntity.ok(attendanceService.getMyMonthlySummary(resolvedYear, resolvedMonth));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> checkIn(
            @Valid @RequestBody AttendanceCheckInRequest request) {
        return new ResponseEntity<>(attendanceService.checkIn(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> checkOut(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceCheckOutRequest request) {
        return ResponseEntity.ok(attendanceService.checkOut(id, request));
    }

    // ── HR / Admin: Manual Entry ──────────────────────────────────────────────

    /**
     * POST /api/v1/company/attendance/manual
     * HR or Admin manually creates an attendance record for any employee / any
     * date.
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> createManual(
            @Valid @RequestBody AttendanceRequest request) {
        return new ResponseEntity<>(attendanceService.createManual(request), HttpStatus.CREATED);
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/company/attendance/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getById(id));
    }

    /**
     * GET /api/v1/company/attendance/employee/{employeeId}?date=2026-07-03
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<AttendanceResponse>> getByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(attendanceService.getByEmployee(
                employeeId,
                PageRequest.of(page, size, Sort.by("attendanceDate").descending())));
    }

    /**
     * GET /api/v1/company/attendance/employee/{employeeId}/date?date=2026-07-03
     * Returns a single attendance record for an employee on a specific date.
     */
    @GetMapping("/employee/{employeeId}/date")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<AttendanceResponse> getByEmployeeAndDate(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getByEmployeeAndDate(employeeId, date));
    }

    /**
     * GET
     * /api/v1/company/attendance/date-range?startDate=2026-07-01&endDate=2026-07-31
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<AttendanceResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(attendanceService.getByCompanyAndDateRange(
                startDate, endDate, PageRequest.of(page, size, Sort.by("attendanceDate").descending())));
    }

    /**
     * GET /api/v1/company/attendance/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<AttendanceResponse>> getByStatus(
            @PathVariable AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(attendanceService.getByStatus(
                status, PageRequest.of(page, size, Sort.by("attendanceDate").descending())));
    }

    @GetMapping("/late")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<AttendanceResponse>> getLateAttendances(@RequestParam LocalDate date) {
        return ResponseEntity.ok(attendanceService.getLateAttendances(date));
    }

    @GetMapping("/absent")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<AttendanceResponse>> getAbsentees(@RequestParam LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAbsentees(date));
    }

    /**
     * POST /api/company/attendance/backfill-absentees?startDate=2026-07-19&endDate=2026-07-21
     *
     * Manually run the absentee marker for the current company over a date range -
     * lets an owner immediately fill in missing ABSENT days (e.g. when the nightly
     * scheduler was offline) instead of waiting for 23:00. Idempotent and scoped to
     * the caller's own company. Returns the number of ABSENT records created.
     */
    @PostMapping("/backfill-absentees")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    public ResponseEntity<Map<String, Object>> backfillAbsentees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "startDate must not be after endDate"));
        }
        // Never process future dates, and cap the span so a stray request can't
        // sweep years of history in one call.
        LocalDate cappedEnd = endDate.isAfter(LocalDate.now()) ? LocalDate.now() : endDate;
        if (startDate.plusDays(366).isBefore(cappedEnd)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "date range must not exceed 366 days"));
        }

        Long companyId = securityUtil.getCurrentCompanyId();
        int created = absenteeMarkingService.backfillForCompany(companyId, startDate, cappedEnd);
        return ResponseEntity.ok(Map.of(
                "created", created,
                "startDate", startDate.toString(),
                "endDate", cappedEnd.toString()));
    }

    // ── Admin Mutations ───────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/company/attendance/{id}/status
     * Body: { "status": "PRESENT" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam AttendanceStatus status) {
        attendanceService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/v1/company/attendance/{id}/approve
     * Fixed: was hardcoding "Admin" as approver name. Now derives name from the
     * authenticated principal via SecurityUtil.
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> approveAttendance(@PathVariable Long id) {
        String approverName = securityUtil.getCurrentUser().getFullName();
        attendanceService.approveAttendance(id, approverName);
        return ResponseEntity.ok().build();
    }
}