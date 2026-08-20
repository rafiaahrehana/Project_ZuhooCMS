package com.businessos.modules.hrm.attendance.attendance;

import com.businessos.enums.LeaveRequestStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.attendance.shift.EmployeeShiftAssignment;
import com.businessos.modules.hrm.attendance.shift.EmployeeShiftAssignmentRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.holiday.HolidayRepository;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Settles attendance for a completed day. Two things count as absence:
 * <ul>
 *   <li>no record at all — the employee never checked in;</li>
 *   <li>a record with a check-in but no check-out — an unfinished day is not
 *       counted as attendance.</li>
 * </ul>
 *
 * Shared by {@code DailyAbsenteeScheduler} (nightly backfill) and the manual
 * admin backfill endpoint.
 *
 * Every marking pass is idempotent: a record already marked ABSENT, or one with
 * both a check-in and a check-out, is left untouched. These are all skipped —
 * company holidays, the employee's weekly-off day (from their assigned shift),
 * approved leave, future dates, and any date before the employee's hire date.
 */
@Service
@RequiredArgsConstructor
public class AbsenteeMarkingService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeShiftAssignmentRepository shiftAssignmentRepository;

    /**
     * Mark absentees for one date across every tenant company. Public so the
     * scheduler can call it once per date through the Spring proxy, giving each
     * date its own transaction — a failure or downtime on one day never skips or
     * rolls back the others.
     *
     * @return number of ABSENT records created
     */
    @Transactional
    public int markAllCompaniesForDate(LocalDate targetDate) {
        int created = 0;
        for (Company company : companyRepository.findAll()) {
            if (company.isPlatformTenant()) continue;
            created += markCompanyForDate(company, targetDate);
        }
        return created;
    }

    /**
     * Backfill absentees for a single company across an inclusive date range —
     * used by the tenant-scoped manual admin trigger.
     *
     * @return number of ABSENT records created
     */
    @Transactional
    public int backfillForCompany(Long companyId, LocalDate start, LocalDate end) {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null || company.isPlatformTenant()) return 0;

        int created = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            created += markCompanyForDate(company, d);
        }
        return created;
    }

    private int markCompanyForDate(Company company, LocalDate targetDate) {
        // Never mark absence for a day that hasn't happened yet.
        if (targetDate.isAfter(LocalDate.now())) return 0;
        if (holidayRepository.existsByCompanyIdAndDate(company.getId(), targetDate)) return 0;

        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        int created = 0;

        for (Employee employee : employeeRepository.findByCompanyIdAndActiveTrue(company.getId())) {
            // Don't invent absences for days before the employee joined.
            if (employee.getHireDate() != null && targetDate.isBefore(employee.getHireDate())) continue;

            List<Attendance> existing =
                    attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), targetDate);
            if (!existing.isEmpty()) {
                created += settleIncompleteDays(existing, targetDate);
                continue;
            }

            if (isWeeklyOff(company.getId(), employee.getId(), dayOfWeek)) continue;

            // Approved leave gets its own ON_LEAVE row rather than no row at
            // all - previously this just skipped the employee entirely, so
            // ON_LEAVE (defined on AttendanceStatus and read by the HR
            // dashboard and attendance reports) was never actually written
            // anywhere, and "% on leave" always read near-zero regardless of
            // how many people were actually out.
            if (leaveRequestRepository.existsApprovedForEmployeeAndDate(
                    employee.getId(), targetDate, LeaveRequestStatus.APPROVED)) {
                attendanceRepository.save(Attendance.builder()
                        .companyId(company.getId())
                        .employee(employee)
                        .attendanceDate(targetDate)
                        .status(AttendanceStatus.ON_LEAVE)
                        .build());
                continue;
            }

            attendanceRepository.save(Attendance.builder()
                    .companyId(company.getId())
                    .employee(employee)
                    .attendanceDate(targetDate)
                    .status(AttendanceStatus.ABSENT)
                    .build());
            created++;
        }
        return created;
    }

    /**
     * A day that was clocked into but never clocked out of does not count as
     * attendance, so it is settled as ABSENT once the day is over.
     *
     * Only past dates are touched: someone who checked in this morning and has
     * not left yet has not failed to check out, and flipping them to ABSENT
     * mid-shift would be wrong. The check-in time is deliberately kept on the
     * record - it is evidence of what happened, and HR may want to correct the
     * day rather than have the history erased.
     *
     * @return number of records flipped to ABSENT
     */
    private int settleIncompleteDays(List<Attendance> existing, LocalDate targetDate) {
        if (!targetDate.isBefore(LocalDate.now())) return 0;

        int settled = 0;
        for (Attendance attendance : existing) {
            if (attendance.getCheckInTime() != null
                    && attendance.getCheckOutTime() == null
                    && attendance.getStatus() != AttendanceStatus.ABSENT) {
                attendance.setStatus(AttendanceStatus.ABSENT);
                // Lateness is a property of a day that was worked. Once the day
                // is settled as absent the employee is not paid for it at all,
                // so carrying a late flag on top both double-counts the same
                // failure and inflates every "late days" figure that counts the
                // flag. The check-in time stays as the evidence of what
                // happened; only the derived lateness is cleared.
                attendance.setLate(false);
                attendance.setLateMinutes(0);
                attendanceRepository.save(attendance);
                settled++;
            }
        }
        return settled;
    }

    private boolean isWeeklyOff(Long companyId, Long employeeId, DayOfWeek dayOfWeek) {
        EmployeeShiftAssignment assignment = shiftAssignmentRepository
                .findByCompanyIdAndEmployeeIdAndActive(companyId, employeeId)
                .orElse(null);
        String weeklyOffDays = assignment != null ? assignment.getShift().getWeeklyOffDays() : "FRI,SAT";
        if (weeklyOffDays == null || weeklyOffDays.isBlank()) return false;

        String abbreviation = dayOfWeek.name().substring(0, 3); // MONDAY -> MON
        List<String> offDays = Arrays.asList(weeklyOffDays.split(","));
        return offDays.contains(abbreviation);
    }
}
