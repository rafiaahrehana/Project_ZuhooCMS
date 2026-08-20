package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

@Entity(name = "WebsiteServiceRequest")
@Table(name = "website_service_requests")
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(unique = true)
    private String code;
    private String name;
    private String email;
    private String phone;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;
    private Long serviceId;
    private String serviceTitle;
    private String status; // SUBMITTED, REVIEW, NEED_DOCUMENTS, APPROVED, COMPLETED
    private LocalDateTime createdAt;
}
