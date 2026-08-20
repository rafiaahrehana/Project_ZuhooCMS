package com.businessos.modules.servicedesk.workflow.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowSuggestionRequest {
    @NotBlank(message = "Goal is required")
    private String goal;
}
