package com.businessos.modules.ai.tool.impl;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.crm.opportunity.OpportunityResponse;
import com.businessos.modules.crm.opportunity.OpportunityService;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckMyCrmDealsTool implements AiTool {

    private final OpportunityService opportunityService;
    private final EmployeeRepository employeeRepository;

    @Override
    public String name() {
        return "check_my_crm_deals";
    }

    @Override
    public String description() {
        return "List the CRM opportunities (deals) owned by the employee - stage, amount, expected close date. Only relevant for sales roles.";
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
    public PermissionCode requiredPermission() {
        return PermissionCode.OPPORTUNITY_VIEW;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        Long employeeId = employeeRepository.findByUserId(userId).map(e -> e.getId()).orElse(null);
        if (employeeId == null) {
            return AiToolResult.failure("You don't have an employee profile set up, so there are no deals to show.");
        }

        List<OpportunityResponse> deals = opportunityService
            .listAll(null, null, employeeId, null, null, PageRequest.of(0, 10, Sort.by("expectedCloseDate").ascending()))
            .getContent();

        if (deals.isEmpty()) {
            return AiToolResult.ok("You don't own any CRM deals right now.", deals);
        }

        StringBuilder sb = new StringBuilder("Your CRM deals:\n");
        for (OpportunityResponse d : deals) {
            sb.append("- ").append(d.getName()).append(" (").append(d.getStage()).append(")");
            if (d.getAmount() != null) sb.append(", ").append(d.getAmount());
            if (d.getExpectedCloseDate() != null) sb.append(", expected close ").append(d.getExpectedCloseDate());
            sb.append('\n');
        }
        return AiToolResult.ok(sb.toString(), deals);
    }
}
