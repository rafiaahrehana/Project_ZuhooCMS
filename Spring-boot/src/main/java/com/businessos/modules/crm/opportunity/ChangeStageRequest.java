package com.businessos.modules.crm.opportunity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangeStageRequest {

    @NotNull(message = "Stage is required")
    private OpportunityStage stage;

    // Required when moving to LOST. The code carries the analysis; the text is
    // the optional detail (required only when the code is OTHER, or "other"
    // becomes a bucket that explains nothing).
    private com.businessos.enums.LostReason lostReasonCode;

    @Size(max = 255)
    private String lostReason;

    // Populated by the frontend's duplicate-detection modal when moving a client-less
    // Opportunity to WON: either link to an existing Client match, or force-create a new one.
    private Long linkToExistingClientId;
    private boolean forceCreateNewClient;
}
