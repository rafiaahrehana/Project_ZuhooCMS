package com.businessos.modules.servicedesk.document;

import com.businessos.modules.servicedesk.companyservice.CompanyService;
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
@Table(name = "required_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequiredDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private CompanyService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String docName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean mandatory = true;

    /** Max age in days — e.g. utility bill must be < 90 days old. */
    private Integer maxAgeDays;

    /** Allowed formats hint for client, e.g. "PDF, JPG, PNG". */
    private String allowedFormats;

    @Builder.Default
    private int sortOrder = 0;
}