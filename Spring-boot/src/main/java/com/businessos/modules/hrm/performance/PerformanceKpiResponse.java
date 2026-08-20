package com.businessos.modules.hrm.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Objective KPIs for one employee over a review period, aggregated from the
 * modules that actually own the data - attendance, leave, service-desk tasks,
 * service requests and client reviews.
 *
 * Nothing here is stored on the review itself. It is recomputed on read, so a
 * review opened today reflects the same period the same way it did last week.
 *
 * Fields are nullable on purpose: null means "no data for this period", which
 * the UI must render as a dash rather than as a zero. An employee with no
 * client reviews has an unknown satisfaction score, not a satisfaction of 0.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceKpiResponse {

    private Long employeeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    /** Days marked PRESENT, LATE or WORK_FROM_HOME. */
    private long daysPresent;
    /** Days marked ABSENT. */
    private long daysAbsent;
    /** Working days with an attendance record of any kind. */
    private long workingDaysRecorded;
    /** daysPresent / workingDaysRecorded, 0-100. Null when nothing was recorded. */
    private Double attendancePercent;

    private long lateArrivals;
    private int  leaveDaysTaken;

    private long tasksCompleted;
    private long projectsCompleted;

    /** Mean client rating (1-5) of this employee's work. Null when unrated. */
    private Double customerSatisfaction;
}
