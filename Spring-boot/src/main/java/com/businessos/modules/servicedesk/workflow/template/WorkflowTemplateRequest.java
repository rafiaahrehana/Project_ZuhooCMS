package com.businessos.modules.servicedesk.workflow.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkflowTemplateRequest {

    @NotBlank(message = "Workflow template name is required")
    @Size(max = 150, message = "Template name must not exceed 150 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
