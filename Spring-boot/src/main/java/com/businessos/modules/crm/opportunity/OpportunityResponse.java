package com.businessos.modules.crm.opportunity;

import com.businessos.enums.LeadSource;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class OpportunityResponse {
    private Long id;
    private String name;
    private String description;
    private OpportunityStage stage;
    private LeadSource source;
    private BigDecimal amount;
    private Integer probability;
    private BigDecimal weightedAmount;
    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    private String nextStep;
    private String lostReason;
    private com.businessos.enums.LostReason lostReasonCode;

    private Long clientId;
    private String clientCompanyName;

    private Long contactId;
    private String contactName;

    private Long sourceLeadId;

    private Long ownerId;
    private String ownerName;

    private LocalDateTime lastActivityAt;
    private LocalDateTime stageChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private java.util.List<com.businessos.modules.crm.tag.TagResponse> tags;

    // Set only when this Opportunity just reached Won and a possible-duplicate Client was
    // linked automatically (informational - see OpportunityServiceImpl.resolveClientForWonDeal).
    private com.businessos.modules.crm.duplicate.DuplicateMatch possibleDuplicate;
}
