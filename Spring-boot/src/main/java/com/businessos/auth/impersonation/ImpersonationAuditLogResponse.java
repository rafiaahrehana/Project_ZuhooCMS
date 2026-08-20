package com.businessos.auth.impersonation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ImpersonationAuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private Long companyId;
    private String companyName;
    private String reason;
    private String impersonationSessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    static ImpersonationAuditLogResponse from(ImpersonationAuditLog log) {
        ImpersonationAuditLogResponse r = new ImpersonationAuditLogResponse();
        r.id = log.getId();
        r.adminId = log.getAdmin() != null ? log.getAdmin().getId() : null;
        r.adminName = log.getAdmin() != null ? log.getAdmin().getFullName() : null;
        r.companyId = log.getCompany() != null ? log.getCompany().getId() : null;
        r.companyName = log.getCompany() != null ? log.getCompany().getCompanyName() : null;
        r.reason = log.getReason();
        r.impersonationSessionId = log.getImpersonationSessionId();
        r.startedAt = log.getStartedAt();
        r.endedAt = log.getEndedAt();
        return r;
    }
}
