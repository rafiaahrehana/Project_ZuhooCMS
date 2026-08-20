package com.businessos.modules.ai.tool.impl;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.enums.LeaveRequestStatus;
import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.ReviewLeaveRequest;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestResponse;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApproveLeaveTool implements AiTool {

    private final LeaveService leaveService;

    @Override
    public String name() {
        return "approve_leave";
    }

    @Override
    public String description() {
        return "Approve or reject a pending leave request, for managers/approvers - identify it by the employee's name or the request id.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "employeeName", Map.of("type", "string", "description", "Name of the employee whose pending leave request this is"),
                "leaveRequestId", Map.of("type", "integer", "description", "Use instead of employeeName if the exact request id is known"),
                "decision", Map.of("type", "string", "enum", List.of("approve", "reject")),
                "rejectionReason", Map.of("type", "string", "description", "Required when decision is reject")
            ),
            "required", List.of("decision")
        );
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public PermissionCode requiredPermission() {
        return PermissionCode.LEAVE_APPROVE;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        String who = args.get("employeeName") != null ? args.get("employeeName").toString()
            : "leave request #" + args.get("leaveRequestId");
        return ("reject".equalsIgnoreCase(String.valueOf(args.get("decision")))
            ? "reject " : "approve ") + who + "'s leave request";
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        if (args == null || args.get("decision") == null) {
            return AiToolResult.failure("I need to know whether to approve or reject, and which request.");
        }

        LeaveRequestResponse target = resolveTarget(args);
        if (target == null) {
            return AiToolResult.failure(args.get("leaveRequestId") != null
                ? "I couldn't find a pending leave request with that id."
                : "I couldn't find a pending leave request matching that name - there may be none, or more than one match.");
        }

        boolean reject = "reject".equalsIgnoreCase(args.get("decision").toString());
        if (reject && (args.get("rejectionReason") == null || args.get("rejectionReason").toString().isBlank())) {
            return AiToolResult.failure("A rejection reason is required to reject a leave request.");
        }

        ReviewLeaveRequest review = new ReviewLeaveRequest();
        review.setStatus(reject ? LeaveRequestStatus.REJECTED : LeaveRequestStatus.APPROVED);
        if (reject) review.setRejectionReason(args.get("rejectionReason").toString());

        try {
            LeaveRequestResponse result = leaveService.review(target.getId(), review);
            return AiToolResult.ok(
                (reject ? "Rejected " : "Approved ") + result.getEmployeeName() + "'s "
                    + result.getLeaveType() + " leave request (" + result.getStartDate() + " to " + result.getEndDate() + ").",
                result);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't complete that: " + e.getMessage());
        }
    }

    private LeaveRequestResponse resolveTarget(Map<String, Object> args) {
        if (args.get("leaveRequestId") instanceof Number n) {
            try {
                LeaveRequestResponse r = leaveService.getById(n.longValue());
                return r.getStatus() == LeaveRequestStatus.PENDING ? r : null;
            } catch (RuntimeException e) {
                // Wrong id, wrong tenant, or already-deleted - all read the same to the caller: not found.
                return null;
            }
        }
        if (args.get("employeeName") == null) return null;

        String name = args.get("employeeName").toString().trim().toLowerCase();
        List<LeaveRequestResponse> pending = leaveService
            .listAll(LeaveRequestStatus.PENDING, PageRequest.of(0, 100))
            .getContent();

        List<LeaveRequestResponse> matches = pending.stream()
            .filter(r -> r.getEmployeeName() != null && r.getEmployeeName().toLowerCase().contains(name))
            .toList();

        return matches.size() == 1 ? matches.get(0) : null;
    }
}
