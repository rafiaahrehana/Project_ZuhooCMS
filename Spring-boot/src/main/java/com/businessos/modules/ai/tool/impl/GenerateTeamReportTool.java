package com.businessos.modules.ai.tool.impl;

import com.businessos.enums.LeaveRequestStatus;
import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.attendance.attendance.AttendanceResponse;
import com.businessos.modules.hrm.attendance.attendance.AttendanceService;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceResponse;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GenerateTeamReportTool implements AiTool {

    private final EmployeeRepository employeeRepository;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;

    @Override
    public String name() {
        return "generate_team_report";
    }

    @Override
    public String description() {
        return "Summarise today's status for the manager's own direct reports - who's checked in today, and who has a low leave balance or a pending leave request.";
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
        Long managerEmployeeId = employeeRepository.findByUserId(userId).map(Employee::getId).orElse(null);
        if (managerEmployeeId == null) {
            return AiToolResult.failure("You don't have an employee profile set up, so there's no team to report on.");
        }

        List<Employee> reports = employeeRepository
            .findByCompanyIdAndReportingManagerIdAndActiveTrue(companyId, managerEmployeeId);
        if (reports.isEmpty()) {
            return AiToolResult.ok("You don't currently have anyone reporting to you.", reports);
        }

        LocalDate today = LocalDate.now();
        List<LeaveRequestResponse> pendingLeaves = leaveService
            .listAll(LeaveRequestStatus.PENDING, PageRequest.of(0, 200))
            .getContent();

        StringBuilder sb = new StringBuilder("Team status for " + today + " (" + reports.size() + " direct report(s)):\n");
        for (Employee e : reports) {
            String name = e.getUser() != null ? e.getUser().getFullName() : "Employee #" + e.getId();
            sb.append("- ").append(name).append(": ");

            String attendance = "no attendance record today";
            try {
                AttendanceResponse a = attendanceService.getByEmployeeAndDate(e.getId(), today);
                if (a != null) attendance = a.getStatus() != null ? a.getStatus().toString() : "checked in";
            } catch (Exception ignored) {
                // No record for today reads the same as an absence for this summary's purposes.
            }
            sb.append(attendance);

            boolean hasPending = pendingLeaves.stream().anyMatch(r -> r.getEmployeeId() != null && r.getEmployeeId().equals(e.getId()));
            if (hasPending) sb.append(", has a pending leave request");

            try {
                List<LeaveBalanceResponse> balances = leaveService.getBalancesForEmployee(e.getId(), today.getYear());
                balances.stream().filter(b -> b.getRemainingDays() <= 2).findFirst()
                    .ifPresent(b -> sb.append(", low ").append(b.getLeaveType()).append(" balance (")
                        .append(b.getRemainingDays()).append(" left)"));
            } catch (Exception ignored) {
                // Leave balances may not be configured for every company.
            }

            sb.append('\n');
        }

        return AiToolResult.ok(sb.toString(), reports);
    }
}
