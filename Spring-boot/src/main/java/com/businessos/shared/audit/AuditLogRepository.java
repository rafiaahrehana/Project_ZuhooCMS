package com.businessos.shared.audit;

import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByCompanyIdOrderByPerformedAtDesc(Long companyId, Pageable pageable);

    Page<AuditLog> findByPerformedByIdOrderByPerformedAtDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(
        AuditEntityType entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndAction(
        Long companyId, AuditAction action, Pageable pageable);
}
