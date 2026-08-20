package com.businessos.modules.servicedesk.approval;

import com.businessos.enums.ApprovalStatus;
import com.businessos.enums.NotificationType;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageApprovalServiceImpl implements StageApprovalService {

    private final StageApprovalRepository approvalRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceRequestService requestService;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public Page<StageApprovalResponse> getPending(Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        return approvalRepository.findByCompanyIdAndStatus(companyId, ApprovalStatus.PENDING, pageable)
                .map(StageApprovalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageApprovalResponse> getForRequest(Long serviceRequestId) {
        Long companyId = securityUtil.getCurrentCompanyId();
        ServiceRequest sr = requestRepository.findByIdAndCompanyId(serviceRequestId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));

        return approvalRepository.findByServiceRequestId(sr.getId()).stream()
                .map(StageApprovalMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StageApprovalResponse approve(Long id, DecisionRequest request) {
        StageApproval approval = getPendingApprovalInTenant(id);

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setDecisionNotes(request.getDecisionNotes());
        approval.setDecidedAt(LocalDateTime.now());
        approval.setDecidedBy(securityUtil.getCurrentUser());

        approvalRepository.save(approval);

        // Advance stage
        requestService.advanceStage(approval.getServiceRequest().getId());

        return StageApprovalMapper.toResponse(approval);
    }

    @Override
    @Transactional
    public StageApprovalResponse reject(Long id, DecisionRequest request) {
        StageApproval approval = getPendingApprovalInTenant(id);

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setDecisionNotes(request.getDecisionNotes());
        approval.setDecidedAt(LocalDateTime.now());
        approval.setDecidedBy(securityUtil.getCurrentUser());

        approvalRepository.save(approval);

        if (approval.getRequestedBy() != null) {
            ServiceRequest sr = approval.getServiceRequest();
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_UPDATED,
                "Stage Approval Rejected",
                "The approval for stage \"" + approval.getWorkflowStage().getName()
                    + "\" on request \"" + sr.getTitle() + "\" was rejected.",
                approval.getRequestedBy().getId(), approval.getCompany().getId(), sr.getId()
            ));
        }

        return StageApprovalMapper.toResponse(approval);
    }

    private StageApproval getPendingApprovalInTenant(Long id) {
        Long companyId = securityUtil.getCurrentCompanyId();
        StageApproval approval = approvalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Approval request is already decided");
        }

        return approval;
    }
}
