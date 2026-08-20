package com.businessos.modules.servicedesk.proposal;

public interface ProposalService {
    ProposalResponse get(Long serviceRequestId);
    ProposalResponse save(Long serviceRequestId, ProposalRequest request);
    ProposalResponse send(Long serviceRequestId);
    ProposalResponse accept(Long serviceRequestId);
    ProposalResponse requestChanges(Long serviceRequestId, String feedback);
    ProposalAttachmentResponse addAttachment(Long serviceRequestId, ProposalAttachmentRequest request);
    void deleteAttachment(Long serviceRequestId, Long attachmentId);
}
