package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.enums.FormFieldType;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_form_fields")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateFormField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_template_id", nullable = false)
    private ServiceTemplate serviceTemplate;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormFieldType fieldType;

    @Builder.Default
    private boolean required = false;

    @Column(columnDefinition = "TEXT")
    private String validationRules;

    @Builder.Default
    private int sortOrder = 0;
}
