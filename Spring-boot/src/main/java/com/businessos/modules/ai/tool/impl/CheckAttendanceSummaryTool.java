package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.attendance.attendance.AttendanceService;
import com.businessos.modules.hrm.attendance.attendance.MyAttendanceMonthlySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckAttendanceSummaryTool implements AiTool {

    private final AttendanceService attendanceService;

    @Override
    public String name() {
        return "check_attendance_summary";
    }

    @Override
    public String description() {
        return "Check the employee's own attendance summary (present/absent/leave/worked hours) for a given month, defaults to the current month.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "year", Map.of("type", "integer"),
                "month", Map.of("type", "integer", "description", "1-12")
            )
        );
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        LocalDate now = LocalDate.now();
        int year = args != null && args.get("year") instanceof Number n ? n.intValue() : now.getYear();
        int month = args != null && args.get("month") instanceof Number n ? n.intValue() : now.getMonthValue();

        MyAttendanceMonthlySummaryResponse s = attendanceService.getMyMonthlySummary(year, month);
        String message = String.format(
            "Attendance for %d-%02d: %d present, %d absent, %d half-day, %d on leave, %d holiday, %d week-off. %s hours worked.",
            year, month, s.getPresentDays(), s.getAbsentDays(), s.getHalfDays(),
            s.getOnLeaveDays(), s.getHolidayDays(), s.getWeekOffDays(),
            s.getWorkedHours() != null ? s.getWorkedHours().toPlainString() : "0");
        return AiToolResult.ok(message, s);
    }
}
