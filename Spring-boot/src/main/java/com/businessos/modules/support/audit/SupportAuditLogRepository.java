package com.businessos.modules.support.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.businessos.shared.audit.AuditLog;

@Repository
public interface SupportAuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityIdOrderByPerformedAtDesc(Long resourceId);

    Page<AuditLog> findByCompanyId(Long companyId, Pageable pageable);
    
    Page<AuditLog> findByCompanyIdAndEntityId(Long companyId, Long resourceId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndAction(Long companyId, com.businessos.enums.AuditAction action, Pageable pageable);

    List<AuditLog> findByCompanyIdAndEntityId(Long companyId, Long resourceId);

    Page<AuditLog> findByCompanyIdAndPerformedAtBetween(Long companyId, java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndPerformedById(Long companyId, Long userId, Pageable pageable);
}
