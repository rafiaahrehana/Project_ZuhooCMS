package com.businessos.modules.servicedesk.proposal;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One proposal per service request - staff edits and re-sends the same row
// rather than versioning, since "here's our latest thinking" is what a client
// wants to see, not a history of drafts.
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "service_proposals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceProposal extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String techStack;

    private String timeline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // Informal, pre-negotiation figure - the binding number is the Quotation
    // that follows once the client accepts this proposal.
    private String estimatedBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String clientFeedback;

    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProposalAttachment> attachments = new ArrayList<>();
}
