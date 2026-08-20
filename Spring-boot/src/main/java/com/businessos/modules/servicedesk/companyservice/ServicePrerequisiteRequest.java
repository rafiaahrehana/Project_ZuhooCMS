package com.businessos.modules.servicedesk.companyservice;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServicePrerequisiteRequest {
    @NotNull
    private Long prerequisiteServiceId;
    private boolean mandatory = true;
    private String message;
}
