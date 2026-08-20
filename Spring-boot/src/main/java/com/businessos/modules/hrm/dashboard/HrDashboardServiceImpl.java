package com.businessos.modules.hrm.dashboard;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.LeaveRequestStatus;
import com.businessos.enums.PayrollStatus;
import com.businessos.enums.JobPostingStatus;
import com.businessos.modules.hrm.attendance.attendance.AttendanceRepository;
import com.businessos.modules.hrm.attendance.attendance.AttendanceStatus;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestRepository;
import com.businessos.modules.hrm.payroll.PayrollRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrDashboardServiceImpl implements HrDashboardService {

    private static final DateTimeFormatter MM_DD = DateTimeFormatter.ofPattern("MM-dd");
    /** How far ahead to look for birthdays and probation endings. */
    private static final int UPCOMING_WINDOW_DAYS = 30;

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final JobPostingRepository jobPostingRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional(readOnly = true)
    public HrDashboardResponse getSummary() {
        // One gate for the whole dashboard. EMPLOYEE_VIEW is the marker for
        // "may see the workforce" - an ordinary employee has their own dashboard
        // and must not read company-wide headcount, payroll or salary totals.
        authorizationService.checkPermission(PermissionCode.EMPLOYEE_VIEW);
        Long companyId = requireCompanyId();

        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        long totalEmployees = employeeRepository.countByCompanyId(companyId);
        long newHires = employeeRepository
                .countByCompanyIdAndActiveTrueAndHireDateBetween(companyId, monthStart, monthEnd);

        long present = attendanceRepository.countByCompanyIdAndStatusAndDate(companyId, AttendanceStatus.PRESENT, today)
                + attendanceRepository.countByCompanyIdAndStatusAndDate(companyId, AttendanceStatus.LATE, today)
                + attendanceRepository.countByCompanyIdAndStatusAndDate(companyId, AttendanceStatus.WORK_FROM_HOME, today);
        long onLeave = attendanceRepository.countByCompanyIdAndStatusAndDate(companyId, AttendanceStatus.ON_LEAVE, today);
        long absent = attendanceRepository.countByCompanyIdAndStatusAndDate(companyId, AttendanceStatus.ABSENT, today);

        // Percentages are of RECORDED attendance, not of headcount: early in the
        // day most rows don't exist yet, and dividing by headcount would show a
        // near-zero attendance rate every morning.
        long recorded = present + onLeave + absent;
        Double presentPct = recorded == 0 ? null : round1(present * 100.0 / recorded);
        Double leavePct = recorded == 0 ? null : round1(onLeave * 100.0 / recorded);

        return HrDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .newHiresThisMonth(newHires)
                .presentToday(present)
                .onLeaveToday(onLeave)
                .absentToday(absent)
                .presentTodayPercent(presentPct)
                .onLeaveTodayPercent(leavePct)
                .openPositions(countOpenPositions(companyId))
                .monthlyPayrollTotal(monthlyPayroll(companyId, month))
                .payrollMonth(month.getMonthValue())
                .payrollYear(month.getYear())
                .departmentDistribution(departmentDistribution(companyId))
                .leaveSummary(leaveSummary(companyId, monthStart, monthEnd))
                .recentJoiners(recentJoiners(companyId))
                .upcomingItems(upcomingItems(companyId, today))
                .headcountTrend(headcountTrend(companyId, monthStart, today))
                .recruitmentPipeline(recruitmentPipeline(companyId))
                .pendingApprovals(pendingApprovals(companyId))
                .build();
    }

    private long countOpenPositions(Long companyId) {
        return jobPostingRepository.findByCompanyIdAndStatus(companyId, JobPostingStatus.OPEN).size();
    }

    /** Net payroll actually paid or approved this month. Null when none exists yet. */
    private BigDecimal monthlyPayroll(Long companyId, YearMonth month) {
        BigDecimal paid = payrollRepository.sumNetSalaryByCompanyAndPeriod(
                companyId, month.getMonthValue(), month.getYear(), PayrollStatus.PAID).orElse(null);
        BigDecimal approved = payrollRepository.sumNetSalaryByCompanyAndPeriod(
                companyId, month.getMonthValue(), month.getYear(), PayrollStatus.APPROVED).orElse(null);

        if (paid == null && approved == null) return null;
        return (paid != null ? paid : BigDecimal.ZERO)
                .add(approved != null ? approved : BigDecimal.ZERO);
    }

    private List<HrDashboardResponse.DepartmentSlice> departmentDistribution(Long companyId) {
        List<Object[]> rows = employeeRepository.countByDepartment(companyId);
        long assigned = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<HrDashboardResponse.DepartmentSlice> slices = new ArrayList<>();
        for (Object[] row : rows) {
            long count = ((Number) row[1]).longValue();
            slices.add(HrDashboardResponse.DepartmentSlice.builder()
                    .department((String) row[0])
                    .count(count)
                    .percent(assigned == 0 ? 0 : round1(count * 100.0 / assigned))
                    .build());
        }
        return slices;
    }

    private HrDashboardResponse.LeaveSummary leaveSummary(Long companyId, LocalDate from, LocalDate to) {
        return HrDashboardResponse.LeaveSummary.builder()
                .total(leaveRequestRepository
                        .countByCompanyIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(companyId, to, from))
                .approved(countLeave(companyId, LeaveRequestStatus.APPROVED, from, to))
                .pending(countLeave(companyId, LeaveRequestStatus.PENDING, from, to))
                .rejected(countLeave(companyId, LeaveRequestStatus.REJECTED, from, to))
                .build();
    }

    private long countLeave(Long companyId, LeaveRequestStatus status, LocalDate from, LocalDate to) {
        return leaveRequestRepository
                .countByCompanyIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        companyId, status, to, from);
    }

    private List<HrDashboardResponse.JoinerItem> recentJoiners(Long companyId) {
        return employeeRepository.findRecentJoiners(companyId, PageRequest.of(0, 5)).stream()
                .map(e -> HrDashboardResponse.JoinerItem.builder()
                        .employeeId(e.getId())
                        .name(displayName(e))
                        .jobTitle(e.getJobTitle())
                        .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                        .hireDate(e.getHireDate())
                        .build())
                .toList();
    }

    /**
     * Birthdays and probation endings in the next 30 days.
     *
     * The birthday window is skipped when it wraps across new year (e.g. 20 Dec
     * to 19 Jan), because the MM-DD string comparison cannot express that range.
     * Returning nothing for those few days is preferable to returning wrong rows.
     */
    private List<HrDashboardResponse.UpcomingItem> upcomingItems(Long companyId, LocalDate today) {
        LocalDate horizon = today.plusDays(UPCOMING_WINDOW_DAYS);
        List<HrDashboardResponse.UpcomingItem> items = new ArrayList<>();

        if (!horizon.isBefore(today) && horizon.getYear() == today.getYear()) {
            for (Employee e : employeeRepository.findBirthdaysBetween(
                    companyId, today.format(MM_DD), horizon.format(MM_DD))) {
                LocalDate next = e.getDateOfBirth().withYear(today.getYear());
                items.add(HrDashboardResponse.UpcomingItem.builder()
                        .kind("BIRTHDAY")
                        .title(displayName(e) + "'s birthday")
                        .subtitle(e.getDepartment() != null ? e.getDepartment().getName() : e.getJobTitle())
                        .date(next)
                        .daysAway(ChronoUnit.DAYS.between(today, next))
                        .build());
            }
        }

        for (Employee e : employeeRepository.findProbationEndingBetween(companyId, today, horizon)) {
            items.add(HrDashboardResponse.UpcomingItem.builder()
                    .kind("PROBATION_END")
                    .title(displayName(e) + " - probation ending")
                    .subtitle(e.getJobTitle())
                    .date(e.getProbationEndDate())
                    .daysAway(ChronoUnit.DAYS.between(today, e.getProbationEndDate()))
                    .build());
        }

        items.sort((a, b) -> Long.compare(a.getDaysAway(), b.getDaysAway()));
        return items.size() > 6 ? items.subList(0, 6) : items;
    }

    /**
     * Month-to-date headcount, reconstructed from hire dates.
     *
     * This is NOT a stored daily snapshot, so it cannot show historical
     * departures - someone who left mid-month was never counted on the earlier
     * days either. It answers "how has the team grown this month", which is what
     * the chart is for. A true history would need a daily headcount table.
     */
    private List<HrDashboardResponse.TrendPoint> headcountTrend(Long companyId, LocalDate from, LocalDate today) {
        List<HrDashboardResponse.TrendPoint> points = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            points.add(HrDashboardResponse.TrendPoint.builder()
                    .date(d)
                    .headcount(employeeRepository.countHiredOnOrBefore(companyId, d))
                    .build());
        }
        return points;
    }

    /**
     * An employee's name lives on their linked User, not on Employee itself.
     * Falls back to the employee number so a record with no user still renders
     * something identifiable rather than a blank row.
     */
    private String displayName(Employee e) {
        if (e.getUser() != null) {
            String full = e.getUser().getFullName();
            if (full != null && !full.isBlank()) return full;
        }
        return e.getEmployeeNumber() != null ? e.getEmployeeNumber() : "Employee #" + e.getId();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    /**
     * The spec's Applied -> Screening -> Interview -> Offer -> Hired funnel.
     * Interview merges the scheduled and completed stages; screening merges
     * shortlisted - five readable stages instead of nine raw statuses.
     */
    private java.util.List<HrDashboardResponse.PipelineStage> recruitmentPipeline(Long companyId) {
        java.util.function.Function<com.businessos.enums.ApplicationStatus, Long> count =
                s -> jobApplicationRepository.countByCompanyIdAndStatus(companyId, s);
        java.util.List<HrDashboardResponse.PipelineStage> stages = new java.util.ArrayList<>();
        stages.add(stage("Applied", count.apply(com.businessos.enums.ApplicationStatus.APPLIED)));
        stages.add(stage("Screening", count.apply(com.businessos.enums.ApplicationStatus.SCREENING)
                + count.apply(com.businessos.enums.ApplicationStatus.SHORTLISTED)));
        stages.add(stage("Interview", count.apply(com.businessos.enums.ApplicationStatus.INTERVIEW_SCHEDULED)
                + count.apply(com.businessos.enums.ApplicationStatus.INTERVIEWED)
                + count.apply(com.businessos.enums.ApplicationStatus.SELECTED)));
        stages.add(stage("Offer", count.apply(com.businessos.enums.ApplicationStatus.OFFER_PENDING)
                + count.apply(com.businessos.enums.ApplicationStatus.OFFER_SENT)
                + count.apply(com.businessos.enums.ApplicationStatus.OFFER_ACCEPTED)));
        stages.add(stage("Hired", count.apply(com.businessos.enums.ApplicationStatus.HIRED)));
        return stages;
    }

    private HrDashboardResponse.PipelineStage stage(String name, long count) {
        return HrDashboardResponse.PipelineStage.builder().stage(name).count(count).build();
    }

    /** Oldest pending leave requests first - the actionable inbox, capped at 6. */
    private java.util.List<HrDashboardResponse.PendingApproval> pendingApprovals(Long companyId) {
        return leaveRequestRepository.findByCompanyIdAndStatus(companyId,
                        com.businessos.enums.LeaveRequestStatus.PENDING,
                        org.springframework.data.domain.PageRequest.of(0, 6,
                                org.springframework.data.domain.Sort.by("createdAt").ascending()))
                .map(lr -> HrDashboardResponse.PendingApproval.builder()
                        .id(lr.getId())
                        .employeeName(lr.getEmployee() != null && lr.getEmployee().getUser() != null
                                ? lr.getEmployee().getUser().getFullName() : "-")
                        .leaveType(lr.getLeaveType() != null ? lr.getLeaveType().name() : "-")
                        .startDate(lr.getStartDate())
                        .endDate(lr.getEndDate())
                        .totalDays(lr.getTotalDays())
                        .build())
                .getContent();
    }
}
