package com.businessos.modules.hrm.performance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PerformanceService {

    PerformanceReviewResponse create(PerformanceReviewRequest request);

    PerformanceReviewResponse getById(Long id);

    Page<PerformanceReviewResponse> listAll(Pageable pageable);

    Page<PerformanceReviewResponse> listForEmployee(Long employeeId, Pageable pageable);

    PerformanceReviewResponse update(Long id, PerformanceReviewRequest request);

    PerformanceReviewResponse finalise(Long id);

    PerformanceReviewResponse advanceStage(Long id);
    PerformanceKpiResponse kpisForEmployee(Long employeeId, java.time.LocalDate from, java.time.LocalDate to);

    java.util.List<PerformanceAttachmentDtos.AttachmentResponse> listAttachments(Long reviewId);
    PerformanceAttachmentDtos.AttachmentResponse addAttachment(
        Long reviewId, PerformanceAttachmentDtos.AttachmentRequest request);
    void deleteAttachment(Long reviewId, Long attachmentId);

    void delete(Long id);

    /** Summarise a review's real scores/notes with AI into a polished narrative */
    PerformanceReviewResponse summarise(Long id);
}
