package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.leave.holiday.HolidayResponse;
import com.businessos.modules.hrm.leave.holiday.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckUpcomingHolidaysTool implements AiTool {

    private final HolidayService holidayService;

    @Override
    public String name() {
        return "check_upcoming_holidays";
    }

    @Override
    public String description() {
        return "List company holidays coming up in the next 90 days.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of("type", "object", "properties", Map.of());
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        LocalDate today = LocalDate.now();
        List<HolidayResponse> holidays = holidayService.listByRange(today, today.plusDays(90));

        if (holidays.isEmpty()) {
            return AiToolResult.ok("No company holidays are scheduled in the next 90 days.", holidays);
        }

        StringBuilder sb = new StringBuilder("Upcoming holidays:\n");
        for (HolidayResponse h : holidays) {
            sb.append("- ").append(h.getHolidayDate()).append(": ").append(h.getName())
              .append(" (").append(h.getHolidayType()).append(")\n");
        }
        return AiToolResult.ok(sb.toString(), holidays);
    }
}
