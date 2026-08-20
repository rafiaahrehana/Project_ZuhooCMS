package com.businessos.modules.hrm.attendance.attendance;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MyAttendanceMonthlySummaryResponse {
    private int year;
    private int month;
    private int presentDays;
    private int absentDays;
    private int halfDays;
    private int onLeaveDays;
    private int holidayDays;
    private int weekOffDays;
    private BigDecimal workedHours;
}
