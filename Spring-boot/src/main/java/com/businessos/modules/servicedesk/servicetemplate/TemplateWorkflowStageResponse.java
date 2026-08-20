package com.businessos.modules.servicedesk.servicetemplate;

import lombok.Data;

@Data
public class TemplateWorkflowStageResponse {
    private Long id;
    private Long serviceTemplateId;
    private String stageName;
    private String stageDescription;
    private int stageOrder;
    private boolean requiresClientAction;
    private boolean requiresPayment;
    private boolean isFinalStage;
}
