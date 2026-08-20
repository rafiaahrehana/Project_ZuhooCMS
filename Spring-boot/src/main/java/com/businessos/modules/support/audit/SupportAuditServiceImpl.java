package com.businessos.modules.support.audit;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportAuditServiceImpl implements SupportAuditService {

    private final SupportAuditLogRepository auditRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public SupportAuditLogResponse getById(Long id) {
        checkTenantPermission();
        com.businessos.shared.audit.AuditLog log = auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()
                && (log.getCompany() == null
                    || !java.util.Objects.equals(log.getCompany().getId(), securityUtil.getCurrentCompanyId()))) {
            throw new ResourceNotFoundException("Audit log not found");
        }
        return SupportAuditLogMapper.toResponse(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAuditLogResponse> getAll(Pageable pageable) {
        checkTenantPermission();
        Long companyId = securityUtil.getCurrentCompanyId();
        return auditRepository.findByCompanyId(companyId, pageable)
                .map(SupportAuditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAuditLogResponse> getByActionType(String actionType, Pageable pageable) {
        checkTenantPermission();
        Long companyId = securityUtil.getCurrentCompanyId();
        com.businessos.enums.AuditAction action = null;
        try {
            action = com.businessos.enums.AuditAction.valueOf(actionType);
        } catch (IllegalArgumentException e) {
            // If actionType doesn't map, return empty page or handle appropriately.
            // For now, let's just let it fail or we could return Page.empty()
            throw new BadRequestException("Invalid action type");
        }
        return auditRepository.findByCompanyIdAndAction(companyId, action, pageable)
                .map(SupportAuditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportAuditLogResponse> getByResourceId(Long resourceId) {
        checkTenantPermission();
        Long companyId = securityUtil.getCurrentCompanyId();
        return auditRepository.findByCompanyIdAndEntityId(companyId, resourceId)
                .stream()
                .map(SupportAuditLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAuditLogResponse> getByDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        checkTenantPermission();
        Long companyId = securityUtil.getCurrentCompanyId();
        return auditRepository.findByCompanyIdAndPerformedAtBetween(companyId, start.atStartOfDay(), end.atStartOfDay(), pageable)
                .map(SupportAuditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAuditLogResponse> getByUser(Long userId, Pageable pageable) {
        checkTenantPermission();
        Long companyId = securityUtil.getCurrentCompanyId();
        return auditRepository.findByCompanyIdAndPerformedById(companyId, userId, pageable)
                .map(SupportAuditLogMapper::toResponse);
    }

    // SUPPORT_MANAGER (platform staff, no CustomRole) also reads these via its own
    // role-based @PreAuthorize - only gate the tenant (COMPANY_OWNER) branch here.
    private void checkTenantPermission() {
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            authorizationService.checkPermission(PermissionCode.AUDIT_LOG_VIEW);
        }
    }
}
