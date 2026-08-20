package com.businessos.modules.support.ticket;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.support.agent.SupportAgent;
import com.businessos.modules.support.category.SupportCategory;
import com.businessos.modules.company.Company;
import com.businessos.auth.user.User;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_company", columnList = "company_id"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_priority", columnList = "priority"),
        @Index(name = "idx_ticket_assigned", columnList = "assigned_to_agent_id"),
        @Index(name = "idx_ticket_created_by", columnList = "created_by_id")
}, uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "ticket_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket extends BaseEntity {

    @Column(name = "company_id")
    private Long companyId; // Company that created the ticket

    @Column(name = "ticket_number", nullable = false)
    private String ticketNumber; // TKT-2024-00001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TicketType ticketType = TicketType.PLATFORM_SUPPORT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy; // Tenant platformuser who created ticket

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client; // Optional: points to CRM Client (B2C)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_employee_id")
    private Employee assignedEmployee; // Optional: points to internal Employee (B2C)

    private String title;
    private String description;

    // Optional screenshot/image attached when the ticket was raised - was
    // declared on SupportTicketRequest but never actually persisted anywhere.
    private String attachmentUrl;
    private String attachmentFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private SupportCategory category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TicketStatus status = TicketStatus.NEW;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TicketSource source = TicketSource.PORTAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_agent_id")
    private SupportAgent assignedToAgent; // Support agent handling

    private LocalDateTime assignedDate;

    // SLA Tracking
    private LocalDateTime firstResponseTime;
    private LocalDateTime resolutionTime;

    private LocalDateTime firstResponseDeadline;
    private LocalDateTime resolutionDeadline;

    @Builder.Default
    private boolean slaBreached = false;

    private String slaBreachReason;

    // Resolution
    private String resolutionNotes;
    private LocalDateTime closedDate;
    private String closedBy;

    // Customer satisfaction
    private Integer satisfactionRating; // 1-5 stars
    private String satisfactionFeedback;

    // Escalation
    @Builder.Default
    private Integer escalationLevel = 1; // 1=First level, 2=Senior, 3=Manager
    private LocalDateTime escalatedDate;
    private String escalationReason;

    @Builder.Default
    private boolean requiresFollowUp = false;

    private LocalDate followUpDate;

    public void assignToAgent(SupportAgent agent) {
        this.assignedToAgent = agent;
        this.assignedDate = LocalDateTime.now();
        this.status = TicketStatus.OPEN;
    }

    public void recordFirstResponse() {
        if (this.firstResponseTime == null) {
            this.firstResponseTime = LocalDateTime.now();
        }
    }

    public void resolve(String notes, String closedByName) {
        this.resolutionNotes = notes;
        this.resolutionTime = LocalDateTime.now();
        this.closedDate = LocalDateTime.now();
        this.closedBy = closedByName;
        this.status = TicketStatus.RESOLVED;
    }

    public void close() {
        this.status = TicketStatus.CLOSED;
    }

    public boolean isOverdueSLA() {
        if (firstResponseDeadline != null && LocalDateTime.now().isAfter(firstResponseDeadline)) {
            return true;
        }
        if (resolutionDeadline != null && LocalDateTime.now().isAfter(resolutionDeadline)) {
            return true;
        }
        return false;
    }

    public long getResponseTimeMinutes() {
        if (firstResponseTime == null || getCreatedAt() == null) return -1;
        return java.time.temporal.ChronoUnit.MINUTES.between(getCreatedAt(), firstResponseTime);
    }

    public long getResolutionTimeMinutes() {
        if (resolutionTime == null || getCreatedAt() == null) return -1;
        return java.time.temporal.ChronoUnit.MINUTES.between(getCreatedAt(), resolutionTime);
    }
}