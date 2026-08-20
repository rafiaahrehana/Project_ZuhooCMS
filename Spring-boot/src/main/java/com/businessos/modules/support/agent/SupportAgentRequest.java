package com.businessos.modules.support.agent;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PACKAGE) @Builder
public class SupportAgentRequest {

    private Long userId;

    private String department;
    private String specialization;

    @Builder.Default
    private SupportAgentStatus status = SupportAgentStatus.ACTIVE;

    @Min(value = 1)
    @Builder.Default
    private int maxConcurrentTickets = 10;

    private String notes;
}
