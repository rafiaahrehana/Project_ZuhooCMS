package com.businessos.shared.audit;

import com.businessos.auth.user.User;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.modules.company.Company;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String oldValue,
            String newValue,
            User performedBy,
            Long companyId,
            String clientIp) {

        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .company(companyStub(companyId))
                .ipAddress(clientIp)
                .build();
        auditLogRepository.save(entry);
    }

    @Async
    @Transactional
    public void logLogin(User user, Long companyId, String clientIp) {
        AuditLog entry = AuditLog.builder()
                .entityType(AuditEntityType.USER)
                .entityId(user.getId())
                .action(AuditAction.LOGIN)
                .newValue(user.getEmail())
                .performedBy(user)
                .company(companyStub(companyId))
                .ipAddress(clientIp)
                .build();
        auditLogRepository.save(entry);
    }

    @Async
    @Transactional
    public void logLogout(User user, Long companyId, String clientIp) {
        AuditLog entry = AuditLog.builder()
                .entityType(AuditEntityType.USER)
                .entityId(user.getId())
                .action(AuditAction.LOGOUT)
                .performedBy(user)
                .company(companyStub(companyId))
                .ipAddress(clientIp)
                .build();
        auditLogRepository.save(entry);
    }

    private Company companyStub(Long companyId) {
        if (companyId == null) return null;
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
