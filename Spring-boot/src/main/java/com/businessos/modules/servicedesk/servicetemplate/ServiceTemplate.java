package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(precision = 12, scale = 2)
    private BigDecimal defaultPrice;

    private Integer estimatedDays;

    private String iconUrl;

    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "serviceTemplate", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<TemplateFormField> formFields = new ArrayList<>();

    @OneToMany(mappedBy = "serviceTemplate", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<TemplateRequiredDocument> requiredDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "serviceTemplate", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    @Builder.Default
    private List<TemplateWorkflowStage> workflowStages = new ArrayList<>();
}
