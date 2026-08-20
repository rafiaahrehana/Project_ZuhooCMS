package com.businessos.modules.servicedesk.servicetemplate;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal defaultPrice;
    private Integer estimatedDays;
    private String iconUrl;
    private boolean active;

    private List<TemplateFormFieldResponse> formFields;
    private List<TemplateRequiredDocumentResponse> requiredDocuments;
    private List<TemplateWorkflowStageResponse> workflowStages;
}
