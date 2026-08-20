package com.businessos.modules.hrm.recruitment.candidate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Candidate> findByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    @Query("select c from Candidate c where c.company.id = :companyId and ("
        + "lower(c.name) like lower(concat('%', :q, '%')) "
        + "or lower(c.email) like lower(concat('%', :q, '%')) "
        + "or lower(c.skills) like lower(concat('%', :q, '%')))")
    Page<Candidate> search(@Param("companyId") Long companyId, @Param("q") String q, Pageable pageable);

    Page<Candidate> findByCompanyId(Long companyId, Pageable pageable);

    /** Unpaged - used by RecruitmentSkillController to pool skill tags for autocomplete. */
    List<Candidate> findByCompanyId(Long companyId);

    long countByCompanyId(Long companyId);
}
