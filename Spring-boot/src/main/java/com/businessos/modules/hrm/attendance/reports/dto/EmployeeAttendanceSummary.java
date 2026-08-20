package com.businessos.modules.hrm.attendance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceSummary {
    private Long employeeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private long presentDays;
    private long lateDays;
    private long absentDays;
    private double attendancePercentage;
}
