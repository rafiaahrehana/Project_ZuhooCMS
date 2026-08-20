package com.businessos.modules.hrm.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewAttachmentRepository
        extends JpaRepository<PerformanceReviewAttachment, Long> {

    List<PerformanceReviewAttachment> findByReviewIdAndCompanyIdOrderByCreatedAtDesc(Long reviewId, Long companyId);

    /** Tenant-scoped lookup - never resolve an attachment by id alone. */
    Optional<PerformanceReviewAttachment> findByIdAndCompanyId(Long id, Long companyId);
}
