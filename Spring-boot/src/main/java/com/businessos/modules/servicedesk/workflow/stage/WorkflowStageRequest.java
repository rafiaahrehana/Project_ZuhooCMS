package com.businessos.modules.servicedesk.workflow.stage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * BUG-FIX SUMMARY
 * ───────────────
 * assigneeRole changed from Role enum to String — WorkflowStage.assigneeRole is annotated
 * @Column(length = 100) and declared as String. Storing a Role enum here is wrong because:
 *   a) The column is a plain VARCHAR — no @Enumerated annotation on the base field.
 *   b) WorkflowStage is a workflow configuration object; the assignee role is a flexible
 *      label (e.g. "ADMIN", "STAFF", "REVIEWER") that may not map 1:1 to the Role enum,
 *      especially as the system evolves.
 *   c) The mapper does stage.setAssigneeRole(request.getAssigneeRole()) — if request has
 *      Role and base has String, this is a compile error.
 *
 * Keeping it as String preserves the existing database schema and gives maximum flexibility.
 * The Angular frontend can validate against a fixed list on its end.
 */
@Data
public class WorkflowStageRequest {

    @NotBlank(message = "Stage name is required")
    @Size(max = 100, message = "Stage name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Stage order is required")
    @Min(value = 1, message = "Stage order must be at least 1")
    private Integer stageOrder;

    @Min(value = 1, message = "Estimated days must be at least 1")
    private Integer estimatedDays;

    @Min(value = 1, message = "SLA hours must be at least 1")
    private Integer slaHours;

    private Boolean requiresApproval = false;

    private String assigneeRole;        // fixed: was Role enum — base stores String

    private Boolean requiresPayment = false;

    @Min(value = 1, message = "Payment percent must be at least 1")
    @jakarta.validation.constraints.Max(value = 100, message = "Payment percent cannot exceed 100")
    private Integer paymentPercent;
}
