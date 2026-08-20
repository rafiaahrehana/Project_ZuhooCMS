package com.businessos.modules.hrm.performance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class PerformanceAttachmentDtos {

    /**
     * The file is uploaded first via POST /api/upload, which returns a URL.
     * This request only records that result against the review.
     */
    @Data
    public static class AttachmentRequest {
        @NotBlank(message = "File name is required")
        private String fileName;
        @NotBlank(message = "File URL is required")
        private String fileUrl;
        private String fileType;
        private Long fileSizeBytes;
        private String label;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        private Long id;
        private Long reviewId;
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSizeBytes;
        private String label;
        private String uploadedByName;
        private LocalDateTime createdAt;
    }

    public static AttachmentResponse toResponse(PerformanceReviewAttachment a) {
        if (a == null) return null;
        return AttachmentResponse.builder()
                .id(a.getId())
                .reviewId(a.getReview() != null ? a.getReview().getId() : null)
                .fileName(a.getFileName())
                .fileUrl(a.getFileUrl())
                .fileType(a.getFileType())
                .fileSizeBytes(a.getFileSizeBytes())
                .label(a.getLabel())
                .uploadedByName(a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
