package com.businessos.modules.crm.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Convert a Qualified Lead into an Opportunity. No Client is created at this point. */
@Getter @Setter
public class ConvertToOpportunityRequest {

    @NotBlank(message = "Opportunity name is required")
    private String opportunityName;

    @NotNull(message = "Expected value is required")
    private BigDecimal expectedValue;

    @NotNull(message = "Expected close date is required")
    private LocalDate expectedCloseDate;
}
