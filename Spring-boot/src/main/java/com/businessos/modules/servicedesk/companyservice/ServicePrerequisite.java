package com.businessos.modules.servicedesk.companyservice;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_prerequisites")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServicePrerequisite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private CompanyService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_service_id", nullable = false)
    private CompanyService prerequisiteService;

    @Builder.Default
    private boolean mandatory = true;

    @Column(columnDefinition = "TEXT")
    private String message;
}
