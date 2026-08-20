package com.businessos.modules.servicedesk.workflow.stage;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * BUG-FIX SUMMARY
 * ───────────────
 * assigneeRole changed from Role enum to String — matches WorkflowStage base field
 * type and WorkflowStageRequest. The mapper sets r.setAssigneeRole(s.getAssigneeRole())
 * where s.getAssigneeRole() returns String. Using Role here would be a compile error.
 */
@Getter
@Setter
public class WorkflowStageResponse {
    private Long id;
    private String name;
    private String description;
    private Integer stageOrder;
    private Integer estimatedDays;
    private Integer slaHours;
    private Boolean requiresApproval;
    private String assigneeRole;        // fixed: was Role enum
    private Boolean requiresPayment;
    private Integer paymentPercent;
    private LocalDateTime createdAt;
}
