package com.businessos.modules.servicedesk.dynamicform;

import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.enums.FormFieldType;
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
@Table(name = "service_form_fields")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceFormField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private CompanyService service;

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
