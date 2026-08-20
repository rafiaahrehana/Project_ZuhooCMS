package com.businessos.modules.servicedesk.servicereview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceReviewRequest {

    @NotNull(message = "Service request ID is required")
    private Long serviceRequestId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    private String comment;
}
