package com.businessos.auth.authentication.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Completes signup for a Google account that has no user yet.
 *
 * The token is sent again and verified again — the previous /api/auth/google call proves nothing
 * about this one, and identity must never be carried across requests by the client.
 */
@Getter
@Setter
public class GoogleRegisterRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    @NotNull(message = "Please choose a company")
    private Long companyId;

    @Size(max = 30, message = "Phone is too long")
    private String phone;

    @Size(max = 150, message = "Company name is too long")
    private String clientCompanyName;

    @Size(max = 100, message = "Industry is too long")
    private String industry;

    @Size(max = 255, message = "Website is too long")
    private String website;
}
