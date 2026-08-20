package com.businessos.auth.impersonation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImpersonationResponse {
    private String accessToken;
    private Long companyId;
    private String companyName;
    private String impersonationSessionId;
    private long expiresInSeconds;
}
