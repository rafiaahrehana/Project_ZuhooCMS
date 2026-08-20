package com.businessos.modules.hrm.attendance.reports.service;

import com.businessos.modules.hrm.attendance.reports.dto.*;

import java.time.LocalDate;

public interface AttendanceReportService {
    DailyAttendanceReport generateDailyReport(LocalDate date);
    MonthlyAttendanceReport generateMonthlyReport(int month, int year);
    EmployeeAttendanceSummary generateEmployeeSummary(Long employeeId, LocalDate start, LocalDate end);
    DepartmentAttendanceReport generateDepartmentReport(String department, LocalDate date);
    LateAndAbsentReport generateLateAbsentReport(LocalDate date);
}
