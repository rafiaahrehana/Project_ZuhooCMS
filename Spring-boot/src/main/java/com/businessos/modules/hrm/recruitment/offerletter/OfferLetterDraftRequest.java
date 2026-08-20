package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.enums.LetterType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfferLetterDraftRequest {
    // Supply employeeId (employment letters) or jobApplicationId (OFFER/APPOINTMENT).
    private Long employeeId;
    private Long jobApplicationId;

    @NotNull(message = "letterType is required")
    private LetterType letterType;
}
