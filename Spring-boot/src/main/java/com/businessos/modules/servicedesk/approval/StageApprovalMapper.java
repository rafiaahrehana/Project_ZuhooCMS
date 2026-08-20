package com.businessos.modules.servicedesk.approval;

public class StageApprovalMapper {
    public static StageApprovalResponse toResponse(StageApproval entity) {
        if (entity == null) {
            return null;
        }

        return StageApprovalResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .approverRole(entity.getApproverRole())
                .decisionNotes(entity.getDecisionNotes())
                .decidedAt(entity.getDecidedAt())
                .serviceRequestId(entity.getServiceRequest() != null ? entity.getServiceRequest().getId() : null)
                .serviceRequestTitle(entity.getServiceRequest() != null ? entity.getServiceRequest().getTitle() : null)
                .workflowStageId(entity.getWorkflowStage() != null ? entity.getWorkflowStage().getId() : null)
                .workflowStageName(entity.getWorkflowStage() != null ? entity.getWorkflowStage().getName() : null)
                .stageOrder(entity.getWorkflowStage() != null ? entity.getWorkflowStage().getStageOrder() : null)
                .requestedByName(entity.getRequestedBy() != null ? entity.getRequestedBy().getFullName() : null)
                .decidedByName(entity.getDecidedBy() != null ? entity.getDecidedBy().getFullName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
