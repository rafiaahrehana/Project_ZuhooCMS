package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckLeaveBalanceTool implements AiTool {

    private final LeaveService leaveService;

    @Override
    public String name() {
        return "check_leave_balance";
    }

    @Override
    public String description() {
        return "Check the employee's own remaining leave balance by type (annual, sick, casual, etc) for the current year.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "year", Map.of("type", "integer", "description", "Defaults to the current year if omitted")
            )
        );
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        int year = args != null && args.get("year") instanceof Number n
            ? n.intValue() : LocalDate.now().getYear();

        // getMyBalances() resolves the caller from SecurityUtil internally -
        // userId/companyId aren't passed through, they're just this tool's
        // proof (per AiTool's contract) that the agent loop already
        // authenticated the caller before reaching here.
        List<LeaveBalanceResponse> balances = leaveService.getMyBalances(year);
        if (balances.isEmpty()) {
            return AiToolResult.ok("No leave balance records found for " + year + ".", balances);
        }

        StringBuilder sb = new StringBuilder("Leave balances for " + year + ":\n");
        for (LeaveBalanceResponse b : balances) {
            sb.append("- ").append(b.getLeaveType()).append(": ")
              .append(b.getRemainingDays()).append(" of ").append(b.getEntitledDays())
              .append(" days remaining (").append(b.getUsedDays()).append(" used, ")
              .append(b.getPendingDays()).append(" pending)\n");
        }
        return AiToolResult.ok(sb.toString(), balances);
    }
}
