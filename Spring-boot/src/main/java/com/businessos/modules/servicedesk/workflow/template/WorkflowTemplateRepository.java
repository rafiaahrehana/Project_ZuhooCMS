package com.businessos.modules.servicedesk.workflow.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * BUG-FIX SUMMARY
 * ───────────────
 * TENANT ISOLATION BUG — Added findByCompanyId(companyId, pageable).
 *
 * WorkflowServiceImpl.listTemplates() called templateRepository.findAll(pageable) which
 * returns every WorkflowTemplate across ALL tenants. This is a critical data leak in a
 * multi-tenant SaaS: Company A could see Company B's workflow configurations.
 *
 * The fix adds findByCompanyId() so WorkflowServiceImpl can scope the query correctly.
 * The existing findAll(Pageable) is removed to prevent accidental future use.
 *
 * findByActiveTrue() also leaked cross-tenant data — replaced with
 * findByCompanyIdAndActiveTrue() for the same reason.
 */
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {

    // Scoped queries — always filter by companyId
    Page<WorkflowTemplate> findByCompanyId(Long companyId, Pageable pageable);

    List<WorkflowTemplate> findByCompanyIdAndActiveTrue(Long companyId); // fixed: was findByActiveTrue()

    Optional<WorkflowTemplate> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
}
