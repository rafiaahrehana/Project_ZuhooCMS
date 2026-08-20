package com.businessos.modules.servicedesk.proposal;

import java.util.Collections;
import java.util.List;

public class ProposalMapper {

    public static ProposalAttachmentResponse toResponse(ProposalAttachment a) {
        ProposalAttachmentResponse r = new ProposalAttachmentResponse();
        r.setId(a.getId());
        if (a.getProposal() != null) r.setProposalId(a.getProposal().getId());
        r.setFileName(a.getFileName());
        r.setFileUrl(a.getFileUrl());
        r.setLabel(a.getLabel());
        return r;
    }

    public static ProposalResponse toResponse(ServiceProposal p) {
        ProposalResponse r = new ProposalResponse();
        r.setId(p.getId());
        if (p.getServiceRequest() != null) r.setServiceRequestId(p.getServiceRequest().getId());
        r.setTitle(p.getTitle());
        r.setTechStack(p.getTechStack());
        r.setTimeline(p.getTimeline());
        r.setSummary(p.getSummary());
        r.setEstimatedBudget(p.getEstimatedBudget());
        r.setStatus(p.getStatus());
        r.setClientFeedback(p.getClientFeedback());
        r.setCreatedByName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null);
        r.setSentAt(p.getSentAt());
        r.setRespondedAt(p.getRespondedAt());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        List<ProposalAttachmentResponse> attachments = p.getAttachments() == null
            ? Collections.emptyList()
            : p.getAttachments().stream()
                .filter(a -> !a.isDeleted())
                .map(ProposalMapper::toResponse)
                .toList();
        r.setAttachments(attachments);
        return r;
    }
}
