package com.businessos.modules.crm.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CrmActivityRequest {

    @NotNull(message = "Activity type is required")
    private CrmActivityType type;

    @NotBlank(message = "Subject is required")
    @Size(max = 200)
    private String subject;

    private String description;

    private LocalDateTime activityDate;

    private LocalDateTime scheduledAt;

    private Boolean completed;

    // At least one of clientId / opportunityId is required
    private Long clientId;

    private Long opportunityId;
}
