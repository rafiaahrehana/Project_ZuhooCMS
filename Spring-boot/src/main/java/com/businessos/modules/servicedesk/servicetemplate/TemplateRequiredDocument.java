package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_required_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateRequiredDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_template_id", nullable = false)
    private ServiceTemplate serviceTemplate;

    @Column(nullable = false)
    private String docName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean mandatory = true;

    private Integer maxAgeDays;

    private String allowedFormats;

    @Builder.Default
    private int sortOrder = 0;
}
