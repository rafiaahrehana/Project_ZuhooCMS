package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.servicedesk.task.Task;
import com.businessos.modules.company.Company;
import com.businessos.enums.ServiceRequestPriority;
import com.businessos.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "service_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ServiceRequest extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceRequestPriority priority = ServiceRequestPriority.NORMAL;

    private LocalDate deadline;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal agreedPrice;

    @Builder.Default
    private Integer currentStage = 0;

    // SLA tracking
    private Integer slaHours;
    private LocalDateTime slaDeadline;
    @Builder.Default
    private boolean slaBreach = false;

    // Government reference
    private String govRefNumber;
    private String govRefType;

    // Embedded Quotation tracking
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal quotationAmount;
    
    @Builder.Default
    private String quotationCurrency = "USD";
    
    @Column(columnDefinition = "TEXT")
    private String quotationNotes;
    
    private LocalDateTime quotationValidUntil;
    
    @Enumerated(EnumType.STRING)
    private com.businessos.enums.QuotationStatus quotationStatus;

    // Client rating after completion
    private Integer clientRating;

    @Column(columnDefinition = "TEXT")
    private String clientFeedback;

    private LocalDateTime ratedAt;

    /**
     * Client answers to the service's dynamic form fields, keyed by
     * ServiceFormField id, serialized as a JSON object.
     */
    @Column(columnDefinition = "TEXT")
    private String formDataJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_service_id", nullable = false)
    private CompanyService companyService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    /**
     * Set when this request is raised under a package subscription.
     * NULL = standalone pay-per-request.
     * When set, ServiceRequestServiceImpl calls packageService.consumeQuota()
     * to decrement the subscriber's remaining request count.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private PackageSubscription subscription;

    @OneToMany(mappedBy = "serviceRequest", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @Builder.Default
    private int resubmitCount = 0;

    @Builder.Default
    private boolean permanentlyClosed = false;

    private Long invoiceId;

    public void submitQuotation(java.math.BigDecimal amount, String currency, String notes, LocalDateTime validUntil) {
        this.quotationAmount = amount;
        if (currency != null) this.quotationCurrency = currency;
        this.quotationNotes = notes;
        this.quotationValidUntil = validUntil;
        this.quotationStatus = com.businessos.enums.QuotationStatus.PENDING;
        this.status = ServiceRequestStatus.QUOTATION_PENDING;
    }

    public void acceptQuotation() {
        this.quotationStatus = com.businessos.enums.QuotationStatus.ACCEPTED;
        this.agreedPrice = this.quotationAmount;
        this.status = ServiceRequestStatus.PENDING;
    }

    public void rejectQuotation(String reason) {
        this.quotationStatus = com.businessos.enums.QuotationStatus.REJECTED;
        this.clientFeedback = reason;
        this.status = ServiceRequestStatus.PENDING;
    }
}
