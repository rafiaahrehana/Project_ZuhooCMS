package com.businessos.modules.servicedesk.companyservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * RENAMED: HubServiceRepository → CompanyServiceRepository
 * All internal references updated. No logic changes.
 */
public interface CompanyServiceRepository extends JpaRepository<CompanyService, Long> {

    Page<CompanyService> findByCompanyId(Long companyId, Pageable pageable);

    Page<CompanyService> findByCompanyIdAndCategoryId(Long companyId, Long categoryId, Pageable pageable);

    List<CompanyService> findByCompanyIdAndActiveTrue(Long companyId);

    Optional<CompanyService> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long excludeId);
}
