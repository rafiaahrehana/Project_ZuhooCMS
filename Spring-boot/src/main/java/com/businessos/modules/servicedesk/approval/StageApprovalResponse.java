package com.businessos.modules.servicedesk.approval;

import com.businessos.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StageApprovalResponse {
    private Long id;
    private ApprovalStatus status;
    private String approverRole;
    private String decisionNotes;
    private LocalDateTime decidedAt;
    private Long serviceRequestId;
    private String serviceRequestTitle;
    private Long workflowStageId;
    private String workflowStageName;
    private Integer stageOrder;
    private String requestedByName;
    private String decidedByName;
    private LocalDateTime createdAt;
}
