package com.businessos.modules.hrm.announcement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementDraftRequest {
    @NotBlank(message = "Instructions are required")
    private String instructions;
}
