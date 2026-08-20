package com.businessos.modules.hrm.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the HR dashboard renders, in one round trip.
 *
 * Aggregated live on read. Nothing here is stored, so the numbers always agree
 * with the underlying employee, attendance, leave and payroll records rather
 * than with a snapshot that has since drifted.
 *
 * Counts that could legitimately be zero are primitives. Values that may be
 * genuinely UNKNOWN are boxed and left null so the UI can show a dash - an
 * attendance rate of null ("nothing recorded today") must not render as 0%.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrDashboardResponse {

    // ── Headline figures ──────────────────────────────────────
    private long totalEmployees;
    private long newHiresThisMonth;
    private long presentToday;
    private long onLeaveToday;
    private long absentToday;
    private long openPositions;

    /** Share of today's recorded attendance that is present. Null when nothing is recorded yet. */
    private Double presentTodayPercent;
    /** Share on leave today. Null when nothing is recorded yet. */
    private Double onLeaveTodayPercent;

    /** Net payroll for the current month. Null when no payroll has been generated. */
    private BigDecimal monthlyPayrollTotal;
    private int payrollMonth;
    private int payrollYear;

    // ── Panels ────────────────────────────────────────────────
    private List<DepartmentSlice> departmentDistribution;
    private LeaveSummary leaveSummary;
    private List<JoinerItem> recentJoiners;
    private List<UpcomingItem> upcomingItems;
    /** Month-to-date headcount, one point per elapsed day. */
    private List<TrendPoint> headcountTrend;
    /** Applications by stage: Applied -> Screening -> Interview -> Offer -> Hired. */
    private List<PipelineStage> recruitmentPipeline;
    /** Oldest pending leave requests, the actionable inbox. */
    private List<PendingApproval> pendingApprovals;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PipelineStage {
        private String stage;
        private long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PendingApproval {
        private Long id;
        private String employeeName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentSlice {
        private String department;
        private long count;
        /** Share of employees who have a department assigned. */
        private double percent;
    }

    /** Leave requests raised in the current month, by outcome. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeaveSummary {
        private long total;
        private long approved;
        private long pending;
        private long rejected;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class JoinerItem {
        private Long employeeId;
        private String name;
        private String jobTitle;
        private String department;
        private LocalDate hireDate;
    }

    /**
     * A dated item needing HR's attention. `kind` is BIRTHDAY or PROBATION_END -
     * both derived from employee records.
     *
     * Deliberately does NOT include "performance review cycle" or "payroll run
     * date": neither is scheduled anywhere in the system, so any date shown for
     * them would be invented.
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpcomingItem {
        private String kind;
        private String title;
        private String subtitle;
        private LocalDate date;
        private long daysAway;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private long headcount;
    }
}
