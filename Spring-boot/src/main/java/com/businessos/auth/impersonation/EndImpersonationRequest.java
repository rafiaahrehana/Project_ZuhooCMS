package com.businessos.auth.impersonation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EndImpersonationRequest {

    @NotBlank(message = "impersonationSessionId is required")
    private String impersonationSessionId;
}
