package com.businessos.modules.servicedesk.proposal;

import com.businessos.auth.user.User;
import com.businessos.enums.CommentVisibility;
import com.businessos.enums.NotificationType;
import com.businessos.modules.servicedesk.requestcomment.RequestComment;
import com.businessos.modules.servicedesk.requestcomment.RequestCommentRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProposalServiceImpl implements ProposalService {

    private final ServiceProposalRepository proposalRepository;
    private final ProposalAttachmentRepository attachmentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final RequestCommentRepository commentRepository;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public ProposalResponse get(Long serviceRequestId) {
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        return proposalRepository.findByServiceRequestIdAndCompanyId(sr.getId(), requireCompanyId())
            .map(ProposalMapper::toResponse)
            .orElse(null);
    }

    @Override
    @Transactional
    public ProposalResponse save(Long serviceRequestId, ProposalRequest request) {
        requireStaff();
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        Long companyId = requireCompanyId();

        ServiceProposal proposal = proposalRepository
            .findByServiceRequestIdAndCompanyId(sr.getId(), companyId)
            .orElse(null);

        if (proposal == null) {
            proposal = ServiceProposal.builder()
                .serviceRequest(sr)
                .company(sr.getCompany())
                .createdBy(securityUtil.getCurrentUser())
                .status(ProposalStatus.DRAFT)
                .build();
        } else if (proposal.getStatus() == ProposalStatus.SENT || proposal.getStatus() == ProposalStatus.ACCEPTED) {
            throw new BadRequestException(
                "Cannot edit a proposal that has already been " + proposal.getStatus().name().toLowerCase()
                    + " - withdraw it first by leaving it as-is, or wait for the client's response.");
        }

        proposal.setTitle(request.getTitle());
        proposal.setTechStack(request.getTechStack());
        proposal.setTimeline(request.getTimeline());
        proposal.setSummary(request.getSummary());
        proposal.setEstimatedBudget(request.getEstimatedBudget());

        proposalRepository.save(proposal);
        return ProposalMapper.toResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse send(Long serviceRequestId) {
        requireStaff();
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        ServiceProposal proposal = requireProposal(sr, requireCompanyId());

        if (proposal.getStatus() != ProposalStatus.DRAFT && proposal.getStatus() != ProposalStatus.CHANGES_REQUESTED) {
            throw new BadRequestException("Proposal is already " + proposal.getStatus().name().toLowerCase());
        }

        proposal.setStatus(ProposalStatus.SENT);
        proposal.setSentAt(LocalDateTime.now());
        proposal.setClientFeedback(null);
        proposalRepository.save(proposal);

        if (sr.getClient() != null && sr.getClient().getUser() != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_UPDATED,
                "Project proposal ready for review",
                "A proposal for \"" + sr.getTitle() + "\" is ready - review the approach, timeline and estimated budget.",
                sr.getClient().getUser().getId(),
                sr.getCompany().getId(),
                sr.getId()));

            postComment(sr, "A project proposal (\"" + proposal.getTitle()
                + "\") is ready for your review.", CommentVisibility.CLIENT);
        }

        return ProposalMapper.toResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse accept(Long serviceRequestId) {
        requireClient();
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        ServiceProposal proposal = requireProposal(sr, requireCompanyId());

        if (proposal.getStatus() != ProposalStatus.SENT) {
            throw new BadRequestException("Only a sent proposal can be accepted");
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setRespondedAt(LocalDateTime.now());
        proposalRepository.save(proposal);

        notifyStaff(sr, "Proposal accepted",
            "\"" + sr.getTitle() + "\" - the client accepted the proposal. Submit a formal quotation to move forward.");
        postComment(sr, "Proposal accepted. We'll follow up with a formal quotation.", CommentVisibility.INTERNAL);

        return ProposalMapper.toResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse requestChanges(Long serviceRequestId, String feedback) {
        requireClient();
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        ServiceProposal proposal = requireProposal(sr, requireCompanyId());

        if (proposal.getStatus() != ProposalStatus.SENT) {
            throw new BadRequestException("Only a sent proposal can have changes requested");
        }

        proposal.setStatus(ProposalStatus.CHANGES_REQUESTED);
        proposal.setClientFeedback(feedback);
        proposal.setRespondedAt(LocalDateTime.now());
        proposalRepository.save(proposal);

        notifyStaff(sr, "Client requested changes to the proposal",
            "\"" + sr.getTitle() + "\" - " + (feedback != null && !feedback.isBlank()
                ? feedback : "the client asked for changes to the proposal."));
        postComment(sr, "Requested changes: " + (feedback != null && !feedback.isBlank()
            ? feedback : "(no details provided)"), CommentVisibility.INTERNAL);

        return ProposalMapper.toResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalAttachmentResponse addAttachment(Long serviceRequestId, ProposalAttachmentRequest request) {
        requireStaff();
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        ServiceProposal proposal = requireProposal(sr, requireCompanyId());

        ProposalAttachment attachment = ProposalAttachment.builder()
            .proposal(proposal)
            .company(sr.getCompany())
            .fileName(request.getFileName())
            .fileUrl(request.getFileUrl())
            .label(request.getLabel())
            .build();

        attachmentRepository.save(attachment);
        return ProposalMapper.toResponse(attachment);
    }

    @Override
    @Transactional
    public void deleteAttachment(Long serviceRequestId, Long attachmentId) {
        requireStaff();
        findRequestInTenant(serviceRequestId);
        ProposalAttachment attachment = attachmentRepository.findByIdAndCompanyId(attachmentId, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        attachment.softDelete();
        attachmentRepository.save(attachment);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ServiceProposal requireProposal(ServiceRequest sr, Long companyId) {
        return proposalRepository.findByServiceRequestIdAndCompanyId(sr.getId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("No proposal exists for this request yet"));
    }

    private void notifyStaff(ServiceRequest sr, String title, String message) {
        if (sr.getAssignedEmployee() != null && sr.getAssignedEmployee().getUser() != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_UPDATED, title, message,
                sr.getAssignedEmployee().getUser().getId(), sr.getCompany().getId(), sr.getId()));
        }
    }

    private void postComment(ServiceRequest sr, String content, CommentVisibility visibility) {
        commentRepository.save(RequestComment.builder()
            .content(content)
            .visibility(visibility)
            .serviceRequest(sr)
            .company(sr.getCompany())
            .author(securityUtil.getCurrentUser())
            .build());
    }

    private ServiceRequest findRequestInTenant(Long id) {
        ServiceRequest sr = serviceRequestRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + id));
        guardAccess(sr);
        return sr;
    }

    private void guardAccess(ServiceRequest sr) {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null) return;
        if (!user.getRole().name().equals("CLIENT")) return;

        boolean isOwner = sr.getClient() != null
            && sr.getClient().getUser() != null
            && sr.getClient().getUser().getId().equals(user.getId());
        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to access this service request");
        }
    }

    private void requireStaff() {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null
                || (!user.getRole().name().equals("COMPANY_OWNER") && !user.getRole().name().equals("EMPLOYEE"))) {
            throw new ForbiddenException("Only staff can manage proposals");
        }
    }

    private void requireClient() {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null || !user.getRole().name().equals("CLIENT")) {
            throw new ForbiddenException("Only the client can respond to a proposal");
        }
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
