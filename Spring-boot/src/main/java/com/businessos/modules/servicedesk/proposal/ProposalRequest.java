package com.businessos.modules.servicedesk.proposal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProposalRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String techStack;
    private String timeline;
    private String summary;
    private String estimatedBudget;
}
