package com.businessos.modules.hrm.recruitment.talentpool;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TalentPoolRepository extends JpaRepository<TalentPoolCandidate, Long> {

    Optional<TalentPoolCandidate> findByIdAndCompanyId(Long id, Long companyId);

    Page<TalentPoolCandidate> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    /** Unpaged - used by RecruitmentSkillController to pool skill tags for autocomplete. */
    List<TalentPoolCandidate> findByCompanyId(Long companyId);

    boolean existsByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    @Query("""
        SELECT c FROM TalentPoolCandidate c
        WHERE c.company.id = :companyId
          AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.desiredRole) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY c.createdAt DESC
        """)
    Page<TalentPoolCandidate> search(@Param("companyId") Long companyId,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
