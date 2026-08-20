package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByCompanyId(Long companyId);
    Optional<Service> findBySlugAndCompanyId(String slug, Long companyId);
    List<Service> findByCompanyIdAndCategoryNameIgnoreCase(Long companyId, String categoryName);
    List<Service> findByCompanyIdAndTitleContainingIgnoreCase(String title, Long companyId);
}
