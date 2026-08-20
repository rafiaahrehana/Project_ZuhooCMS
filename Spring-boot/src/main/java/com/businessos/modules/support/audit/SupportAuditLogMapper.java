package com.businessos.modules.support.audit;

import com.businessos.shared.audit.AuditLog;

public class SupportAuditLogMapper {

    public static SupportAuditLogResponse toResponse(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return SupportAuditLogResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .actionByUserId(entity.getPerformedBy() != null ? entity.getPerformedBy().getId() : null)
                .actionByUserName(entity.getPerformedBy() != null ? entity.getPerformedBy().getFullName() : null)
                .actionType(entity.getAction() != null ? entity.getAction().name() : null)
                .resourceId(entity.getEntityId())
                .resourceType(entity.getEntityType() != null ? entity.getEntityType().name() : null)
                .description(entity.getAction() != null ? entity.getAction().name() : null)
                .changes(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .userAgent(null)
                .contextSwitchToCompanyId(null)
                .contextSwitchToCompanyName(null)
                .createdAt(entity.getPerformedAt())
                .build();
    }
}
