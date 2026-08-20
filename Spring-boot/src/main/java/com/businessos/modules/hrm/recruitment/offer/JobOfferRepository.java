package com.businessos.modules.hrm.recruitment.offer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    Optional<JobOffer> findByIdAndCompanyId(Long id, Long companyId);

    Page<JobOffer> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    Page<JobOffer> findByCompanyIdAndStatusOrderByCreatedAtDesc(
        Long companyId, JobOffer.Status status, Pageable pageable);

    List<JobOffer> findByJobApplicationIdOrderByCreatedAtDesc(Long jobApplicationId);

    boolean existsByJobApplicationIdAndStatusIn(Long jobApplicationId, List<JobOffer.Status> statuses);

    /** Unpaged - RecruitmentKpiServiceImpl computes company-wide, per-job and per-recruiter offer-acceptance rates from one fetch rather than N count queries. */
    List<JobOffer> findByCompanyId(Long companyId);
}
