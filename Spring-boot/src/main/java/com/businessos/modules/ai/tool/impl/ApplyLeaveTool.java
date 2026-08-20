package com.businessos.modules.ai.tool.impl;

import com.businessos.enums.LeaveType;
import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestDto;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestResponse;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Write tool - the agent loop (AiServiceImpl#runAgentTurn) never calls
 * execute() on the first pass regardless of what this returns; it always
 * proposes the parsed args back to the employee and only executes after an
 * explicit confirmation message. This class has no confirmation logic of its
 * own - that gate lives once, in the loop, so it can't be forgotten per-tool.
 */
@Component
@RequiredArgsConstructor
public class ApplyLeaveTool implements AiTool {

    private final LeaveService leaveService;

    @Override
    public String name() {
        return "apply_leave";
    }

    @Override
    public String description() {
        return "Submit a leave request for the employee. Does not approve it - goes to the normal approval queue like applying from the Leave Requests page.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "leaveType", Map.of("type", "string", "enum",
                    java.util.Arrays.stream(LeaveType.values()).map(Enum::name).toList(),
                    "description", "One of: " + java.util.Arrays.toString(LeaveType.values())),
                "startDate", Map.of("type", "string", "format", "date"),
                "endDate", Map.of("type", "string", "format", "date"),
                "reason", Map.of("type", "string")
            ),
            "required", java.util.List.of("leaveType", "startDate", "endDate")
        );
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        String reason = args.get("reason") != null ? " (\"" + args.get("reason") + "\")" : "";
        return "submit " + args.get("leaveType") + " leave from " + args.get("startDate")
            + " to " + args.get("endDate") + reason;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        if (args == null || args.get("leaveType") == null
                || args.get("startDate") == null || args.get("endDate") == null) {
            return AiToolResult.failure("Missing leave type, start date, or end date.");
        }

        LeaveRequestDto dto = new LeaveRequestDto();
        try {
            dto.setLeaveType(LeaveType.valueOf(args.get("leaveType").toString().toUpperCase()));
            dto.setStartDate(LocalDate.parse(args.get("startDate").toString()));
            dto.setEndDate(LocalDate.parse(args.get("endDate").toString()));
        } catch (Exception e) {
            return AiToolResult.failure("Couldn't understand the leave type or dates: " + e.getMessage());
        }
        Object reason = args.get("reason");
        if (reason != null) dto.setReason(reason.toString());

        try {
            // apply() resolves the employee from SecurityUtil and enforces
            // every existing rule (balance, overlap, no-backdating, policy
            // max-consecutive-days) exactly as the Leave Requests page does.
            LeaveRequestResponse response = leaveService.apply(dto);
            return AiToolResult.ok(
                "Submitted: " + dto.getLeaveType() + " leave from " + dto.getStartDate()
                    + " to " + dto.getEndDate() + ", now pending approval (request #"
                    + response.getId() + ").",
                response);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't submit that leave request: " + e.getMessage());
        }
    }
}
