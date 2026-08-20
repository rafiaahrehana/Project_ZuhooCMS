package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_workflow_stages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateWorkflowStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_template_id", nullable = false)
    private ServiceTemplate serviceTemplate;

    @Column(nullable = false)
    private String stageName;

    @Column(columnDefinition = "TEXT")
    private String stageDescription;

    private int stageOrder;

    @Builder.Default
    private boolean requiresClientAction = false;
    
    @Builder.Default
    private boolean requiresPayment = false;
    
    @Builder.Default
    private boolean isFinalStage = false;
}
