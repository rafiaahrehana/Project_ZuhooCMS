package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.attendance.attendance.AttendanceCheckOutRequest;
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
public class CheckOutTool implements AiTool {

    private final AttendanceService attendanceService;

    @Override
    public String name() {
        return "check_out";
    }

    @Override
    public String description() {
        return "Clock the employee out for today, right now - requires having checked in already.";
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
        return "clock you out for today, right now";
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        AttendanceResponse today;
        try {
            today = attendanceService.getMyTodayAttendance();
        } catch (Exception e) {
            return AiToolResult.failure("You haven't checked in today, so there's nothing to check out of.");
        }
        if (today == null) {
            return AiToolResult.failure("You haven't checked in today, so there's nothing to check out of.");
        }

        AttendanceCheckOutRequest request = new AttendanceCheckOutRequest();
        request.setCheckOutTime(LocalTime.now());
        request.setMethod(AttendanceMethod.MANUAL);
        try {
            AttendanceResponse response = attendanceService.checkOut(today.getId(), request);
            return AiToolResult.ok("Checked out at " + response.getCheckOutTime() + ".", response);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't check you out: " + e.getMessage());
        }
    }
}
