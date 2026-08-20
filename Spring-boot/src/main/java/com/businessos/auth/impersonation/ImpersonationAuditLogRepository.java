package com.businessos.auth.impersonation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImpersonationAuditLogRepository extends JpaRepository<ImpersonationAuditLog, Long> {

    Optional<ImpersonationAuditLog> findByImpersonationSessionId(String impersonationSessionId);

    // Was write-only - no endpoint anywhere read these back, undermining the
    // whole point of a compliance record for one of the most sensitive actions
    // a platform admin can take.
    Page<ImpersonationAuditLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    Page<ImpersonationAuditLog> findByCompanyIdOrderByStartedAtDesc(Long companyId, Pageable pageable);
}
