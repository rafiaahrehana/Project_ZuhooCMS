package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.performance.PerformanceReviewResponse;
import com.businessos.modules.hrm.performance.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckPerformanceReviewTool implements AiTool {

    private final PerformanceService performanceService;
    private final EmployeeRepository employeeRepository;

    @Override
    public String name() {
        return "check_performance_review";
    }

    @Override
    public String description() {
        return "Show the employee's most recent performance review - scores, strengths, areas for improvement.";
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
            return AiToolResult.failure("You don't have an employee profile set up, so there's no review to show.");
        }

        List<PerformanceReviewResponse> reviews = performanceService
            .listForEmployee(employeeId, PageRequest.of(0, 1, Sort.by("createdAt").descending()))
            .getContent();

        if (reviews.isEmpty()) {
            return AiToolResult.ok("You don't have any performance reviews on record yet.", reviews);
        }

        PerformanceReviewResponse r = reviews.get(0);
        StringBuilder sb = new StringBuilder("Your most recent performance review (");
        sb.append(r.getReviewPeriodStart()).append(" to ").append(r.getReviewPeriodEnd()).append("):\n");
        if (r.getOverallScore() != null) sb.append("- Overall score: ").append(r.getOverallScore()).append('\n');
        if (r.getPerformanceLevel() != null) sb.append("- Performance level: ").append(r.getPerformanceLevel()).append('\n');
        sb.append("- Stage: ").append(r.getStage()).append(r.isFinalised() ? " (finalised)" : " (not yet finalised)").append('\n');
        if (r.getStrengths() != null && !r.getStrengths().isBlank()) sb.append("- Strengths: ").append(r.getStrengths()).append('\n');
        if (r.getAreasForImprovement() != null && !r.getAreasForImprovement().isBlank()) sb.append("- Areas for improvement: ").append(r.getAreasForImprovement()).append('\n');
        if (r.getGoalsForNextPeriod() != null && !r.getGoalsForNextPeriod().isBlank()) sb.append("- Goals for next period: ").append(r.getGoalsForNextPeriod()).append('\n');

        return AiToolResult.ok(sb.toString(), r);
    }
}
