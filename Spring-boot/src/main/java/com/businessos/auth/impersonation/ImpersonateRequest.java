package com.businessos.auth.impersonation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImpersonateRequest {

    @NotBlank(message = "A reason is required to access a company")
    private String reason;
}
