package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.attendance.attendance.AttendanceCheckInRequest;
import com.businessos.modules.hrm.attendance.attendance.AttendanceMethod;
import com.businessos.modules.hrm.attendance.attendance.AttendanceResponse;
import com.businessos.modules.hrm.attendance.attendance.AttendanceService;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckInTool implements AiTool {

    private final AttendanceService attendanceService;

    @Override
    public String name() {
        return "check_in";
    }

    @Override
    public String description() {
        return "Clock the employee in for today, right now.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of("type", "object", "properties", Map.of());
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        return "clock you in for today, right now";
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        AttendanceCheckInRequest request = new AttendanceCheckInRequest();
        request.setCheckInTime(LocalTime.now());
        request.setMethod(AttendanceMethod.MANUAL);
        try {
            AttendanceResponse response = attendanceService.checkIn(request);
            return AiToolResult.ok("Checked in at " + response.getCheckInTime() + ".", response);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't check you in: " + e.getMessage());
        }
    }
}
