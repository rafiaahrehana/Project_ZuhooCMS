package com.businessos.modules.servicedesk.approval;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.enums.ApprovalStatus;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stage_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageApproval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_stage_id", nullable = false)
    private WorkflowStage workflowStage;

    @Column(name = "approver_role")
    private String approverRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String decisionNotes;
    
    private java.time.LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;
}
