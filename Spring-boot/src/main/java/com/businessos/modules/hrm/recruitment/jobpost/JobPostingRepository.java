package com.businessos.modules.hrm.recruitment.jobpost;


import com.businessos.enums.JobPostingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface JobPostingRepository extends JpaRepository <JobPosting, Long> {

    Page<JobPosting> findByCompanyId(Long companyId, Pageable pageable);

    /** Unpaged - used by RecruitmentKpiServiceImpl to join against the full application set in Java. */
    List<JobPosting> findByCompanyId(Long companyId);

    Page<JobPosting> findByCompanyIdAndStatus(Long companyId, JobPostingStatus status, Pageable pageable);

    List<JobPosting> findByCompanyIdAndStatus(Long companyId, JobPostingStatus status);

    Optional<JobPosting> findByIdAndCompanyId(Long id, Long companyId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE JobPosting j SET j.status = :closed
        WHERE j.deadline < :today
          AND j.status = :open
          AND j.deleted = false
        """)
    int closeExpiredPostings(
        @Param("today") LocalDate today,
        @Param("open") JobPostingStatus open,
        @Param("closed") JobPostingStatus closed
    );

    List<JobPosting> findByCompanyIdAndStatusAndDeadlineGreaterThanEqual(
        Long companyId, JobPostingStatus status, LocalDate today);
}
