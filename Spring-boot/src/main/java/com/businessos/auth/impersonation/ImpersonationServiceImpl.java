package com.businessos.auth.impersonation;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.JwtService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.audit.AuditService;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ImpersonationServiceImpl implements ImpersonationService {

    private final CompanyRepository companyRepository;
    private final ImpersonationAuditLogRepository impersonationAuditLogRepository;
    private final JwtService jwtService;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    @Override
    public ImpersonationResponse startImpersonation(Long companyId, ImpersonateRequest request) {
        User admin = securityUtil.getCurrentUser();

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtService.generateImpersonationToken(
            admin.getEmail(), Role.COMPANY_OWNER.name(), companyId, admin.getId(), sessionId);

        impersonationAuditLogRepository.save(ImpersonationAuditLog.builder()
            .admin(admin)
            .company(company)
            .reason(request.getReason())
            .impersonationSessionId(sessionId)
            .startedAt(LocalDateTime.now())
            .build());

        // ImpersonationAuditLog is a separate table the Support Audit Logs page
        // never reads - impersonation, one of the most sensitive actions a
        // platform admin can take, was invisible on the one screen built to
        // review sensitive actions. Mirrors the same call added to
        // SupportContextSwitchServiceImpl.switchContext() for the equivalent gap.
        auditService.log(AuditEntityType.COMPANY, company.getId(), AuditAction.ASSIGN,
                null, "Impersonation started by " + admin.getEmail()
                        + (request.getReason() != null ? " - " + request.getReason() : ""),
                admin, companyId, null);

        return new ImpersonationResponse(
            accessToken,
            companyId,
            company.getCompanyName(),
            sessionId,
            jwtService.getImpersonationExpirationMs() / 1000);
    }

    @Override
    public void endImpersonation(EndImpersonationRequest request) {
        ImpersonationAuditLog log = impersonationAuditLogRepository
            .findByImpersonationSessionId(request.getImpersonationSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Impersonation session not found"));

        if (log.getEndedAt() == null) {
            log.setEndedAt(LocalDateTime.now());
            impersonationAuditLogRepository.save(log);
        }
    }
}
