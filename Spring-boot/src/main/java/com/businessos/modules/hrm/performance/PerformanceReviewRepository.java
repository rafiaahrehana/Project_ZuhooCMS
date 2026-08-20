package com.businessos.modules.hrm.performance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    Optional<PerformanceReview> findByIdAndCompanyId(Long id, Long companyId);

    Page<PerformanceReview> findByCompanyId(Long companyId, Pageable pageable);

    Page<PerformanceReview> findByCompanyIdAndEmployeeId(
        Long companyId, Long employeeId, Pageable pageable);

    // Cross-company (runs outside an HTTP request context - scheduler), matching
    // the convention used by LicenseExpiryScheduler.
    @Query("""
        SELECT r FROM PerformanceReview r
         WHERE r.finalised = false AND r.deleted = false
           AND r.reviewPeriodEnd < :cutoff
           AND r.overdueReminderSentAt IS NULL
        """)
    List<PerformanceReview> findNewlyOverdue(@Param("cutoff") java.time.LocalDate cutoff);
}
