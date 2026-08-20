package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.asset.AssetResponse;
import com.businessos.modules.hrm.asset.AssetService;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckMyAssetsTool implements AiTool {

    private final AssetService assetService;
    private final EmployeeRepository employeeRepository;

    @Override
    public String name() {
        return "check_my_assets";
    }

    @Override
    public String description() {
        return "List the company assets (laptop, monitor, phone, etc.) currently assigned to the employee.";
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
        Long employeeId = employeeRepository.findByUserId(userId).map(e -> e.getId()).orElse(null);
        if (employeeId == null) {
            return AiToolResult.failure("You don't have an employee profile set up, so there's nothing assigned to look up.");
        }

        List<AssetResponse> assets = assetService.listForEmployee(employeeId);
        if (assets.isEmpty()) {
            return AiToolResult.ok("You don't have any company assets assigned to you right now.", assets);
        }

        StringBuilder sb = new StringBuilder("Assets assigned to you:\n");
        for (AssetResponse a : assets) {
            sb.append("- ").append(a.getName());
            if (a.getAssetTag() != null) sb.append(" (").append(a.getAssetTag()).append(")");
            sb.append(" - ").append(a.getCategory());
            if (a.getAssignedAt() != null) sb.append(", assigned ").append(a.getAssignedAt());
            sb.append('\n');
        }
        return AiToolResult.ok(sb.toString(), assets);
    }
}
