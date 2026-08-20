package com.businessos.modules.servicedesk.proposal;

import lombok.Data;

@Data
public class ProposalAttachmentResponse {
    private Long id;
    private Long proposalId;
    private String fileName;
    private String fileUrl;
    private String label;
}
