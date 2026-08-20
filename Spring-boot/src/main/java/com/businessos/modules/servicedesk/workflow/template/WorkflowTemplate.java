package com.businessos.modules.servicedesk.workflow.template;

import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.ArrayList;
import java.util.List;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "workflow_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToMany(mappedBy = "workflowTemplate", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    @Builder.Default
    private List<WorkflowStage> stages = new ArrayList<>();
}
