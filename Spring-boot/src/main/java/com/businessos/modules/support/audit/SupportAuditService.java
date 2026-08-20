package com.businessos.modules.support.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface SupportAuditService {
    SupportAuditLogResponse getById(Long id);
    Page<SupportAuditLogResponse> getAll(Pageable pageable);
    Page<SupportAuditLogResponse> getByActionType(String actionType, Pageable pageable);
    List<SupportAuditLogResponse> getByResourceId(Long resourceId);
    Page<SupportAuditLogResponse> getByDateRange(LocalDate start, LocalDate end, Pageable pageable);
    Page<SupportAuditLogResponse> getByUser(Long userId, Pageable pageable);
}
