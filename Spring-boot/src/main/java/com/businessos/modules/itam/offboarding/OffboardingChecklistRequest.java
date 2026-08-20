package com.businessos.modules.itam.offboarding;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffboardingChecklistRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    private String notes;
}
