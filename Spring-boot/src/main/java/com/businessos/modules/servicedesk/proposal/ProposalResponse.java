package com.businessos.modules.servicedesk.proposal;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProposalResponse {
    private Long id;
    private Long serviceRequestId;
    private String title;
    private String techStack;
    private String timeline;
    private String summary;
    private String estimatedBudget;
    private ProposalStatus status;
    private String clientFeedback;
    private String createdByName;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProposalAttachmentResponse> attachments;
}
