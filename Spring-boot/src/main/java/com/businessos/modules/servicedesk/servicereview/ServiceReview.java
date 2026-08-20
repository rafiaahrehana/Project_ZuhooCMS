package com.businessos.modules.servicedesk.servicereview;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "service_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceReview extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", unique = true, nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private CompanyService hubService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Employee staff;

    @Min(value = 1) @Max(value = 5)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    private boolean published = false;
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String adminResponse;
}
