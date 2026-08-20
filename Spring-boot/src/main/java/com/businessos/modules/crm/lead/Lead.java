package com.businessos.modules.crm.lead;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.core.base.BaseEntity;
import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_lead_company_status", columnList = "company_id,status"),
    @Index(name = "idx_lead_assigned_to", columnList = "assigned_to_id"),
    @Index(name = "idx_lead_source", columnList = "source"),
    @Index(name = "idx_lead_email", columnList = "email"),
    @Index(name = "idx_lead_created_at", columnList = "created_at"),
    @Index(name = "idx_lead_priority", columnList = "priority")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String contactName;

    @Column(length = 150)
    private String companyName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String industry;

    @Column(length = 100)
    private String jobTitle;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status = LeadStatus.NEW;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadSource source = LeadSource.OTHER;

    // Free-text detail when source = OTHER (e.g. "Trade show", "Partner referral network")
    @Column(length = 150)
    private String sourceOther;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Priority priority = Priority.NORMAL;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate expectedCloseDate;
    private LocalDate lastContactDate;
    private LocalDateTime lastActivityAt;

    // Set once CrmFollowUpScheduler notifies the assignee this lead has gone
    // stale; reset to null whenever new activity is logged, so the next
    // staleness period notifies again instead of staying permanently silent.
    private LocalDateTime staleNotifiedAt;

    // Which service they're interested in
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_service_id")
    private CompanyService interestedService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private com.businessos.modules.company.Company company;

    // Assigned sales person
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private com.businessos.modules.hrm.employee.Employee assignedTo;

    // When lead converts to client
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_client_id")
    private Client convertedClient;

    @Builder.Default
    private boolean converted = false;
    private LocalDateTime convertedAt;

    // Bidirectional relationship with activities
    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, orphanRemoval = false)
    @Builder.Default
    private List<com.businessos.modules.crm.activity.CrmActivity> activities = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "lead_tags",
        joinColumns = @JoinColumn(name = "lead_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private List<com.businessos.modules.crm.tag.Tag> tags = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = LeadStatus.NEW;
        }
        if (this.source == null) {
            this.source = LeadSource.OTHER;
        }
        if (this.priority == null) {
            this.priority = Priority.NORMAL;
        }
    }
}
