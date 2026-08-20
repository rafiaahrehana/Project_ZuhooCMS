package com.businessos.modules.ai.repository;

import com.businessos.modules.ai.entity.AiProviderConfig;
import com.businessos.modules.ai.enums.AiProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Long> {

    Optional<AiProviderConfig> findByCompanyIdAndActiveTrue(Long companyId);

    // A company can save one config per provider (uq_ai_config_company_provider) -
    // this is the upsert lookup so re-saving the same provider updates its existing
    // row instead of colliding with the unique constraint.
    Optional<AiProviderConfig> findByCompanyIdAndAiProviderType(Long companyId, AiProviderType aiProviderType);

    List<AiProviderConfig> findByCompanyIdOrderByAiProviderType(Long companyId);
}
