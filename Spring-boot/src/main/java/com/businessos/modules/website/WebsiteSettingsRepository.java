package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WebsiteSettingsRepository extends JpaRepository<WebsiteSettings, Long> {
    Optional<WebsiteSettings> findByCompanyId(Long companyId);
}
