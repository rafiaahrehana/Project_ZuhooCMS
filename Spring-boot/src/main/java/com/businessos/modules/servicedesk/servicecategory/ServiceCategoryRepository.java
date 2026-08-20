package com.businessos.modules.servicedesk.servicecategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByCompanyIdAndActiveTrueOrderBySortOrderAsc(Long companyId);

    List<ServiceCategory> findByCompanyIdOrderBySortOrderAsc(Long companyId);

    Optional<ServiceCategory> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
}
