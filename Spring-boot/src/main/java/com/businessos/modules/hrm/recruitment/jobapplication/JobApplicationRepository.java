package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByIdAndCompanyId(Long id, Long companyId);

    Page<JobApplication> findByCompanyId(Long companyId, Pageable pageable);

    /** Unpaged - used by RecruitmentKpiServiceImpl, which aggregates in Java over the full set rather than issuing one query per breakdown. */
    List<JobApplication> findByCompanyId(Long companyId);

    Page<JobApplication> findByCompanyIdAndJobPostingId(
        Long companyId, Long jobPostingId, Pageable pageable);

    Page<JobApplication> findByCompanyIdAndStatus(
        Long companyId, ApplicationStatus status, Pageable pageable);

    // HR's only recourse for a corrected resubmission used to be deleting the
    // old record - a rejected/withdrawn candidate had no way back in.
    boolean existsByJobPostingIdAndCandidateIdAndStatusNotIn(
        Long jobPostingId, Long candidateId, List<ApplicationStatus> excludedStatuses);

    List<JobApplication> findByCompanyIdAndCandidateId(Long companyId, Long candidateId);

    long countByCompanyIdAndCandidateId(Long companyId, Long candidateId);

    long countByCompanyIdAndJobPostingId(Long companyId, Long jobPostingId);

    long countByCompanyIdAndStatus(Long companyId, com.businessos.enums.ApplicationStatus status);

    long countByCompanyIdAndJobPostingIdAndStatus(Long companyId, Long jobPostingId, com.businessos.enums.ApplicationStatus status);
}
