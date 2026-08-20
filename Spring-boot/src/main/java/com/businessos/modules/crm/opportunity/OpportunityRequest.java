package com.businessos.modules.crm.opportunity;

import com.businessos.enums.LeadSource;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class OpportunityRequest {

    @NotBlank(message = "Opportunity name is required")
    @Size(max = 200)
    private String name;

    private String description;

    // Not @NotNull: required for direct create() (checked in OpportunityServiceImpl),
    // but an Opportunity created from a Lead (createFromLead) has no Client until it's Won.
    private Long clientId;

    private Long contactId;

    private Long ownerId;

    private OpportunityStage stage;

    private LeadSource source;

    @DecimalMin(value = "0.0", message = "Amount cannot be negative")
    private BigDecimal amount;

    @Min(0) @Max(100)
    private Integer probability;

    private LocalDate expectedCloseDate;

    @Size(max = 255)
    private String nextStep;

    private java.util.List<Long> tagIds;
}
