package com.businessos.modules.servicedesk.servicetemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String name;
    private String description;
    private Long categoryId;
    private BigDecimal defaultPrice;
    private Integer estimatedDays;
    private String iconUrl;
    private boolean active;
    
    private List<TemplateFormFieldRequest> formFields;
    private List<TemplateRequiredDocumentRequest> requiredDocuments;
    private List<TemplateWorkflowStageRequest> workflowStages;
}
