package com.businessos.modules.servicedesk.proposal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProposalAttachmentRequest {
    @NotBlank(message = "File name is required")
    private String fileName;
    @NotBlank(message = "File URL is required")
    private String fileUrl;
    private String label;
}
