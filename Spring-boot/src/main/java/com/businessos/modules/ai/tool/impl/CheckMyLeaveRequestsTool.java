package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckMyLeaveRequestsTool implements AiTool {

    private final LeaveService leaveService;

    @Override
    public String name() {
        return "check_my_leave_requests";
    }

    @Override
    public String description() {
        return "List the employee's own recent leave requests and their approval status (pending, approved, rejected).";
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
        List<LeaveRequestResponse> requests = leaveService
            .listMyLeaves(PageRequest.of(0, 10, Sort.by("createdAt").descending()))
            .getContent();

        if (requests.isEmpty()) {
            return AiToolResult.ok("You haven't submitted any leave requests yet.", requests);
        }

        StringBuilder sb = new StringBuilder("Your recent leave requests:\n");
        for (LeaveRequestResponse r : requests) {
            sb.append("- ").append(r.getLeaveType()).append(" ").append(r.getStartDate())
              .append(" to ").append(r.getEndDate()).append(": ").append(r.getStatus()).append('\n');
        }
        return AiToolResult.ok(sb.toString(), requests);
    }
}
