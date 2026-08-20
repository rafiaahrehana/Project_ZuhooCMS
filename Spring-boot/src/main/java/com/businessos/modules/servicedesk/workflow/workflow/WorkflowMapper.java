package com.businessos.modules.servicedesk.workflow.workflow;

import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageResponse;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateResponse;

import java.util.Collections;
import java.util.List;

public class WorkflowMapper {

    public static WorkflowStageResponse toStageResponse(WorkflowStage s) {
        WorkflowStageResponse r = new WorkflowStageResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setDescription(s.getDescription());
        r.setStageOrder(s.getStageOrder());
        r.setEstimatedDays(s.getEstimatedDays());
        r.setSlaHours(s.getSlaHours());
        r.setRequiresApproval(s.getRequiresApproval());
        r.setAssigneeRole(s.getAssigneeRole());
        r.setRequiresPayment(s.getRequiresPayment());
        r.setPaymentPercent(s.getPaymentPercent());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }

    public static WorkflowTemplateResponse toResponse(WorkflowTemplate t) {
        List<WorkflowStageResponse> stages = t.getStages() == null
            ? Collections.emptyList()
            : t.getStages().stream()
                .filter(s -> !s.isDeleted())
                .map(WorkflowMapper::toStageResponse)
                .toList();

        WorkflowTemplateResponse r = new WorkflowTemplateResponse();
        r.setId(t.getId());
        r.setName(t.getName());
        r.setDescription(t.getDescription());
        r.setVersion(t.getVersion());
        r.setActive(t.isActive());
        r.setStages(stages);
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }
}
