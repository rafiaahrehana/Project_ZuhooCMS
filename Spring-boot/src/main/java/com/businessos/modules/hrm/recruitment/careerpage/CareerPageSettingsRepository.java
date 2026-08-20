package com.businessos.modules.hrm.recruitment.careerpage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareerPageSettingsRepository extends JpaRepository<CareerPageSettings, Long> {
    Optional<CareerPageSettings> findByCompanyId(Long companyId);
    Optional<CareerPageSettings> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndCompanyIdNot(String slug, Long companyId);
}
