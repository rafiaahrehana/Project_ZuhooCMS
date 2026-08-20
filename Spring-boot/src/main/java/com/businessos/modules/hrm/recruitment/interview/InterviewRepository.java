package com.businessos.modules.hrm.recruitment.interview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByIdAndCompanyId(Long id, Long companyId);

    Page<Interview> findByCompanyIdOrderByScheduledAtDesc(Long companyId, Pageable pageable);

    Page<Interview> findByCompanyIdAndStatusOrderByScheduledAtAsc(
        Long companyId, Interview.Status status, Pageable pageable);

    List<Interview> findByJobApplicationIdOrderByScheduledAtAsc(Long jobApplicationId);

    boolean existsByJobApplicationIdAndStatus(Long jobApplicationId, Interview.Status status);
}
