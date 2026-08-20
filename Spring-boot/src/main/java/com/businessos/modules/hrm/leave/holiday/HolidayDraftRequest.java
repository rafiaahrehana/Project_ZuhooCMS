package com.businessos.modules.hrm.leave.holiday;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HolidayDraftRequest {
    @NotBlank(message = "Instructions are required")
    private String instructions;
}
