package com.businessos.modules.hrm.attendance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceReport {
    private int month;
    private int year;
    private long totalWorkingDays;
    private long presentCount;
    private long lateCount;
    private long absentCount;
    private double attendancePercentage;
}
