package com.businessos.modules.servicedesk.workflow.stage;

import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "workflow_stages",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"workflow_template_id", "stage_order"},
        name = "uq_stage_order_per_template"
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowStage extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stageOrder;

    private Integer estimatedDays;
    private Integer slaHours;
    private Boolean requiresApproval;

    @Column(length = 100)
    private String assigneeRole;

    // Milestone billing: when a stage completes with this set, advanceStage()
    // notifies the client (and posts a visible comment) that an installment is
    // due - paymentPercent of the request's agreedPrice. Collecting the actual
    // payment stays a manual step (Record Payment / partial invoice), since the
    // system can't safely guess amounts when agreedPrice isn't set yet.
    private Boolean requiresPayment;
    private Integer paymentPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", nullable = false)
    private WorkflowTemplate workflowTemplate;
}
