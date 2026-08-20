package com.businessos.modules.ai.repository;


import com.businessos.modules.ai.entity.AiPromptTemplate;
import com.businessos.modules.ai.enums.AiFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {

    /**
     * Returns the active template for a feature scoped to a company,
     * falling back to the platform default (company IS NULL) if none exists.
     * Company-specific record wins — sorted by company_id DESC NULLS LAST.
     */
    @Query("""
        SELECT t FROM AiPromptTemplate t
        WHERE t.feature = :feature
          AND t.active = true
          AND t.deleted = false
          AND (t.company.id = :companyId OR t.company IS NULL)
        ORDER BY t.company.id DESC NULLS LAST
        """)
    List<AiPromptTemplate> findActiveForFeature(
        @Param("feature") AiFeature feature,
        @Param("companyId") Long companyId);

    Page<AiPromptTemplate> findByCompanyIdOrderByFeatureAscVersionDesc(Long companyId, Pageable pageable);

    Page<AiPromptTemplate> findByCompanyIsNullOrderByFeatureAscVersionDesc(Pageable pageable);
}
