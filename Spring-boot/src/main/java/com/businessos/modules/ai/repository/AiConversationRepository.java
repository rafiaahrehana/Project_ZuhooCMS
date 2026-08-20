package com.businessos.modules.ai.repository;


import com.businessos.modules.ai.entity.AiConversation;
import com.businessos.modules.ai.enums.AiFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Page<AiConversation> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    Page<AiConversation> findByCompanyIdAndFeatureOrderByCreatedAtDesc(
            Long companyId, AiFeature feature, Pageable pageable);

    // Oldest-first so callers can fold it straight into a transcript; capped
    // via Pageable rather than a fixed-size query so the "last N" window is a
    // caller decision, not baked into the repository.
    org.springframework.data.domain.Page<AiConversation> findByThreadIdOrderByCreatedAtAsc(
            Long threadId, Pageable pageable);
}
