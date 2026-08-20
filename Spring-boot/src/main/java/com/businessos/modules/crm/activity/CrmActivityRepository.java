package com.businessos.modules.crm.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {

    Optional<CrmActivity> findByIdAndCompanyId(Long id, Long companyId);

    Page<CrmActivity> findByCompanyIdAndClientIdOrderByActivityDateDesc(Long companyId, Long clientId, Pageable pageable);

    Page<CrmActivity> findByCompanyIdAndOpportunityIdOrderByActivityDateDesc(Long companyId, Long opportunityId, Pageable pageable);

    Page<CrmActivity> findByCompanyIdOrderByActivityDateDesc(Long companyId, Pageable pageable);

    Page<CrmActivity> findByLeadIdAndCompanyId(Long leadId, Long companyId, Pageable pageable);

    // No companyId scoping - runs outside an HTTP request context (scheduler), so the
    // tenant Hibernate filter isn't active; matches the convention used by SLA/invoice
    // schedulers, which likewise scan across all companies.
    List<CrmActivity> findByFollowUpAtLessThanEqualAndFollowUpDoneFalseAndDeletedFalse(LocalDateTime cutoff);

    /** Due, still open, and not yet notified - the set the scheduler acts on. */
    List<CrmActivity> findByFollowUpAtLessThanEqualAndFollowUpDoneFalseAndFollowUpNotifiedAtIsNullAndDeletedFalse(LocalDateTime cutoff);

    List<CrmActivity> findByCompanyIdAndFollowUpDoneFalseAndFollowUpAtGreaterThanEqualOrderByFollowUpAtAsc(
            Long companyId, LocalDateTime from);
}
