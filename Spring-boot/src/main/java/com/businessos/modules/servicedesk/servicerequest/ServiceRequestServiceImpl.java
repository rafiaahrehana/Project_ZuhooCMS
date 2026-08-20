package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.core.automation.AutomationEventPublisher;
import com.businessos.enums.*;
import com.businessos.enums.SubscriptionStatus;
import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.modules.servicedesk.companyservice.PackageSubscriptionRepository;
import com.businessos.modules.servicedesk.companyservice.ServicePackageService;
import com.businessos.modules.servicedesk.requestcomment.AddCommentRequest;
import com.businessos.modules.servicedesk.requeststatus.ChangeRequestStatusRequest;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.modules.servicedesk.requestcomment.RequestCommentResponse;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistoryResponse;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.requestcomment.RequestComment;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistory;
import com.businessos.auth.user.User;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.servicedesk.companyservice.CompanyServiceRepository;
import com.businessos.modules.servicedesk.requestcomment.RequestCommentRepository;
import com.businessos.modules.servicedesk.requeststatus.RequestStatusHistoryRepository;
import com.businessos.modules.servicedesk.task.TaskRepository;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageRepository;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.servicedesk.approval.StageApproval;
import com.businessos.modules.servicedesk.approval.StageApprovalRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.enums.Role;
import com.businessos.auth.role.repository.RolePermissionRepository;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.NotificationService;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.modules.finance.invoice.ClientInvoiceService;
import com.businessos.modules.finance.invoice.ClientInvoiceRequest;
import com.businessos.modules.finance.invoice.ClientInvoiceResponse;
import com.businessos.modules.finance.invoice.ClientInvoiceItemRequest;
import com.businessos.modules.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import com.businessos.modules.ai.support.PreparedPrompt;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.businessos.modules.servicedesk.dynamicform.ServiceFormField;
import com.businessos.modules.servicedesk.dynamicform.ServiceFormFieldRepository;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.ServiceRequestSummaryPromptBuilder;
import com.businessos.modules.ai.prompt.ServiceRequestReplyDraftPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import org.springframework.data.domain.PageRequest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final com.businessos.modules.servicedesk.document.RequiredDocumentRepository requiredDocumentRepository;
    private final com.businessos.modules.servicedesk.companyservice.ServicePrerequisiteRepository servicePrerequisiteRepository;
    private final com.businessos.modules.servicedesk.document.DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final RequestCommentRepository commentRepository;
    private final RequestStatusHistoryRepository historyRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final PackageSubscriptionRepository subscriptionRepository;
    private final ServicePackageService packageService;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final StageApprovalRepository stageApprovalRepository;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final RolePermissionRepository rolePermissionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final CompanyRepository companyRepository;
    private final AutomationEventPublisher automationEventPublisher;
    private final ClientInvoiceService invoiceService;
    private final ServiceFormFieldRepository serviceFormFieldRepository;
    private final ObjectMapper objectMapper;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public ServiceRequestResponse create(CreateServiceRequestRequest request) {
        Long companyId = requireCompanyId();
        User currentUser = securityUtil.getCurrentUser();

        Client client = clientRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new BadRequestException(
                "Only clients can submit service requests"));

        CompanyService service = companyServiceRepository
            .findByIdAndCompanyId(request.getHubServiceId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service not found: " + request.getHubServiceId()));

        if (!service.isActive()) {
            throw new BadRequestException("This service is currently unavailable");
        }

        validatePrerequisites(companyId, client.getId(), service);

        PackageSubscription subscription = null;
        BigDecimal agreedPrice;

        if (request.getSubscriptionId() != null) {
            // Validate the subscription belongs to this client and tenant
            subscription = subscriptionRepository
                .findByIdAndCompanyId(request.getSubscriptionId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Subscription not found: " + request.getSubscriptionId()));

            if (!subscription.getClient().getId().equals(client.getId())) {
                throw new BadRequestException(
                    "This subscription does not belong to you");
            }
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new BadRequestException(
                    "Subscription is not active. Status: " + subscription.getStatus());
            }
            // Verify the requested service is included in the package
            if (!subscription.getServicePackage().includesService(service.getId())) {
                throw new BadRequestException(
                    "Service '" + service.getName() +
                    "' is not included in your subscription package");
            }
            // Consume quota — throws if exhausted
            packageService.consumeQuota(subscription.getId());

            // Included in package — no extra charge
            agreedPrice = BigDecimal.ZERO;

        } else {
            agreedPrice = request.getAgreedPrice() != null
                ? request.getAgreedPrice()
                : service.getPrice();
        }

        // Validate the client's answers against the service's dynamic form
        // definition (required fields must be present, keyed by field id),
        // then persist them as JSON on the request.
        String formDataJson = validateAndSerializeFormData(
            companyId, service.getId(), request.getFormData());

        ServiceRequest sr = ServiceRequest.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(ServiceRequestStatus.PENDING)
            .priority(request.getPriority() != null
                ? request.getPriority() : service.getDefaultPriority())
            .agreedPrice(agreedPrice)
            .slaDeadline(request.getSlaDeadline())
            .formDataJson(formDataJson)
            .company(companyRef(companyId))
            .client(client)
            .companyService(service)
            .subscription(subscription)
            .build();

        serviceRequestRepository.save(sr);
        recordStatusChange(sr, null, ServiceRequestStatus.PENDING,
            "Request submitted", currentUser, companyId);

        // Staff otherwise never learn a new request exists until someone happens to
        // open the Service Requests list - alert whoever can actually handle it.
        try {
            notifyAssignableStaff(companyId, NotificationType.REQUEST_SUBMITTED, "New Service Request",
                client.getClientCompanyName() + " submitted a new request: \"" + sr.getTitle() + "\"", sr.getId());
        } catch (Exception ex) {
            log.warn("New-request staff notification failed for request {}: {}", sr.getId(), ex.getMessage());
        }

        if (client.getUser() != null) {
            try {
                Company fullCompany = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                EmailBranding.Data branding = emailBranding.from(fullCompany);
                emailService.sendTicketCreatedEmail(client.getUser().getEmail(), client.getUser().getFirstName(), sr.getTitle(), branding);
            } catch (Exception ex) {
                log.warn("Ticket created email failed for client {}: {}", client.getUser().getEmail(), ex.getMessage());
            }
        }

        ServiceRequestResponse response = toResponse(sr);

        if (agreedPrice.compareTo(BigDecimal.ZERO) > 0) {
            ClientInvoiceItemRequest item = ClientInvoiceItemRequest.builder()
                .description("Service Request: " + sr.getTitle())
                .quantity(new BigDecimal("1"))
                .unitPrice(agreedPrice)
                .build();

            ClientInvoiceRequest invoiceRequest = ClientInvoiceRequest.builder()
                .clientId(client.getId())
                .serviceRequestId(sr.getId())
                .invoiceDate(java.time.LocalDate.now())
                .dueDate(java.time.LocalDate.now().plusDays(3))
                .notes("Invoice for Service Request: " + sr.getTitle())
                .items(List.of(item))
                .build();

            ClientInvoiceResponse invoiceResponse = invoiceService.createForServiceRequest(companyId, invoiceRequest);
            sr.setInvoiceId(invoiceResponse.getId());
            serviceRequestRepository.save(sr);
            response.setInvoiceId(invoiceResponse.getId());

            invoiceService.sendInvoiceForServiceRequest(invoiceResponse.getId());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse getById(Long id) {
        return toResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listAll(ServiceRequestStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SERVICE_REQUEST_VIEW);
        Long companyId = requireCompanyId();
        Page<ServiceRequest> page = status != null
            ? serviceRequestRepository.findByCompanyIdAndStatus(companyId, status, pageable)
            : serviceRequestRepository.findByCompanyId(companyId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listMyRequests(Pageable pageable) {
        Long companyId = requireCompanyId();
        User currentUser = securityUtil.getCurrentUser();
        Client client = clientRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new BadRequestException("Client profile not found"));
        return serviceRequestRepository
            .findByCompanyIdAndClientId(companyId, client.getId(), pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> listAssignedToMe(Pageable pageable) {
        Long companyId = requireCompanyId();
        User currentUser = securityUtil.getCurrentUser();
        Employee emp = employeeRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        return serviceRequestRepository
            .findByCompanyIdAndAssignedEmployeeId(companyId, emp.getId(), pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional
    public ServiceRequestResponse update(Long id, UpdateServiceRequestRequest request) {
        authorizationService.checkAnyPermission(
            PermissionCode.SERVICE_REQUEST_ASSIGN, PermissionCode.SERVICE_REQUEST_APPROVE,
            PermissionCode.SERVICE_REQUEST_CLOSE);
        ServiceRequest sr = findInTenant(id);
        guardNotClosed(sr);

        if (request.getTitle()       != null) sr.setTitle(request.getTitle());
        if (request.getDescription() != null) sr.setDescription(request.getDescription());
        if (request.getPriority()    != null) sr.setPriority(request.getPriority());
        if (request.getAgreedPrice() != null) sr.setAgreedPrice(request.getAgreedPrice());
        if (request.getSlaDeadline() != null) sr.setSlaDeadline(request.getSlaDeadline());
        if (request.getGovRefNumber() != null) sr.setGovRefNumber(request.getGovRefNumber());
        if (request.getGovRefType() != null) sr.setGovRefType(request.getGovRefType());

        if (request.getAssignedEmployeeId() != null) {
            Employee emp = employeeRepository
                .findByIdAndCompanyId(request.getAssignedEmployeeId(), requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Employee not found: " + request.getAssignedEmployeeId()));
            sr.setAssignedEmployee(emp);
            if (sr.getAssignedAt() == null) sr.setAssignedAt(LocalDateTime.now());
        }
        return toResponse(sr);
    }

    @Override
    @Transactional
    public ServiceRequestResponse changeStatus(Long id, ChangeRequestStatusRequest request) {
        // Terminal transitions (completing/rejecting/cancelling) require the
        // dedicated "close" permission; everything else that advances a request
        // through its lifecycle only needs the (lighter) "approve" permission.
        ServiceRequestStatus targetStatus = request.getStatus();
        boolean isTerminal = targetStatus == ServiceRequestStatus.COMPLETED
            || targetStatus == ServiceRequestStatus.REJECTED
            || targetStatus == ServiceRequestStatus.CANCELLED;
        authorizationService.checkPermission(
            isTerminal ? PermissionCode.SERVICE_REQUEST_CLOSE : PermissionCode.SERVICE_REQUEST_APPROVE);

        ServiceRequest sr = findInTenant(id);
        guardNotClosed(sr);

        ServiceRequestStatus oldStatus = sr.getStatus();
        ServiceRequestStatus newStatus = request.getStatus();
        User currentUser = securityUtil.getCurrentUser();

        if ((oldStatus == ServiceRequestStatus.PENDING || oldStatus == ServiceRequestStatus.QUOTATION_PENDING)
                && (newStatus == ServiceRequestStatus.ASSIGNED || newStatus == ServiceRequestStatus.IN_PROGRESS)) {
            validateRequiredDocuments(sr);
        }

        sr.setStatus(newStatus);
        if (newStatus == ServiceRequestStatus.COMPLETED) {
            // 1. Check for incomplete tasks
            boolean hasIncompleteTasks = sr.getTasks().stream()
                .anyMatch(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED);
            if (hasIncompleteTasks) {
                throw new BadRequestException("Cannot complete service request with active, incomplete tasks");
            }

            // 2. Check for pending stage approvals
            boolean hasPendingApprovals = stageApprovalRepository.findByServiceRequestId(sr.getId()).stream()
                .anyMatch(approval -> approval.getStatus() == ApprovalStatus.PENDING);
            if (hasPendingApprovals) {
                throw new BadRequestException("Cannot complete service request with pending workflow stage approvals");
            }

            sr.setCompletedAt(LocalDateTime.now());
            sr.setPermanentlyClosed(true);
            automationEventPublisher.publishServiceRequestCompleted(
                this, requireCompanyId(), sr.getId(),
                sr.getClient() != null ? sr.getClient().getId() : null);
        }
        if (newStatus == ServiceRequestStatus.ASSIGNED && sr.getAssignedAt() == null)
            sr.setAssignedAt(LocalDateTime.now());

        recordStatusChange(sr, oldStatus, newStatus, request.getReason(),
            currentUser, requireCompanyId());
        notifyClientOnStatusChange(sr, newStatus);
        return toResponse(sr);
    }

    @Override
    @Transactional
    public ServiceRequestResponse assign(Long id, Long employeeId) {
        authorizationService.checkPermission(PermissionCode.SERVICE_REQUEST_ASSIGN);
        Long companyId = requireCompanyId();
        ServiceRequest sr = findInTenant(id);
        guardNotClosed(sr);

        Employee emp = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Employee not found: " + employeeId));

        if (sr.getStatus() == ServiceRequestStatus.PENDING) {
            validateRequiredDocuments(sr);
        }

        if (sr.getAssignedEmployee() != null && sr.getAssignedEmployee().getId().equals(employeeId)) {
            // Already assigned to this employee, no need to duplicate history or emails
            return toResponse(sr);
        }

        ServiceRequestStatus old = sr.getStatus();
        sr.setAssignedEmployee(emp);
        sr.setAssignedAt(LocalDateTime.now());
        sr.setStatus(ServiceRequestStatus.ASSIGNED);

        recordStatusChange(sr, old, ServiceRequestStatus.ASSIGNED,
            "Assigned to " + emp.getUser().getFullName(),
            securityUtil.getCurrentUser(), companyId);

        if (emp.getUser() != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_ASSIGNED,
                "Request Assigned",
                "Service request \"" + sr.getTitle() + "\" has been assigned to you.",
                emp.getUser().getId(), companyId, sr.getId()
            ));

            try {
                Company fullCompany = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                EmailBranding.Data branding = emailBranding.from(fullCompany);
                emailService.sendTicketAssignedEmail(emp.getUser().getEmail(), emp.getUser().getFirstName(), sr.getTitle(), branding);
            } catch (Exception ex) {
                log.warn("Ticket assigned email failed for employee {}: {}", emp.getUser().getEmail(), ex.getMessage());
            }
        }
        return toResponse(sr);
    }

    @Override
    @Transactional
    public void cancel(Long id, String reason) {
        ServiceRequest sr = findInTenant(id);
        guardNotClosed(sr);

        User currentUser = securityUtil.getCurrentUser();
        // Once staff has been assigned, a client can no longer self-cancel - staff
        // is already working the request, so it needs to go through support instead.
        // Staff themselves keep unrestricted cancel (e.g. to force-cancel a stuck one).
        if (currentUser.getRole() == Role.CLIENT && sr.getAssignedEmployee() != null) {
            throw new BadRequestException(
                "Cannot cancel after a team member has been assigned. Please contact support.");
        }

        Long companyId = requireCompanyId();
        ServiceRequestStatus oldStatus = sr.getStatus();
        sr.setStatus(ServiceRequestStatus.CANCELLED);
        sr.setPermanentlyClosed(true);
        // Previously hardcoded regardless of why the caller actually cancelled -
        // changeStatus() (the other cancellation path) already captures the
        // real reason via request.getReason().
        recordStatusChange(sr, oldStatus, ServiceRequestStatus.CANCELLED,
            (reason != null && !reason.isBlank()) ? reason : "Cancelled by platform user",
            currentUser, companyId);

        if (sr.getSubscription() != null) {
            packageService.releaseQuota(sr.getSubscription().getId());
        }

        if (sr.getInvoiceId() != null) {
            invoiceService.cancelOrRefundForServiceRequest(companyId, sr.getInvoiceId());
        }
    }

    @Override
    @Transactional
    public void systemCancelForNonPayment(Long id) {
        ServiceRequest sr = serviceRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + id));
        if (sr.isPermanentlyClosed()) return;

        Long companyId = sr.getCompany().getId();
        ServiceRequestStatus oldStatus = sr.getStatus();
        sr.setStatus(ServiceRequestStatus.CANCELLED);
        sr.setPermanentlyClosed(true);
        recordStatusChange(sr, oldStatus, ServiceRequestStatus.CANCELLED,
            "Automatically cancelled - payment not received within 72 hours", null, companyId);

        if (sr.getSubscription() != null) {
            packageService.releaseQuota(sr.getSubscription().getId());
        }
        if (sr.getInvoiceId() != null) {
            invoiceService.cancelOrRefundForServiceRequest(companyId, sr.getInvoiceId());
        }

        if (sr.getClient() != null && sr.getClient().getUser() != null) {
            try {
                Company fullCompany = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                EmailBranding.Data branding = emailBranding.from(fullCompany);
                emailService.sendServiceRequestCancelledEmail(
                    sr.getClient().getUser().getEmail(), sr.getClient().getUser().getFirstName(),
                    sr.getTitle(), branding);
            } catch (Exception ex) {
                log.warn("Cancellation email failed for service request {} (still cancelled): {}", sr.getId(), ex.getMessage());
            }
        }
    }

    // ── Comments ──────────────────────────────────────────────────

    @Override
    @Transactional
    public RequestCommentResponse addComment(Long requestId, AddCommentRequest request) {
        Long companyId = requireCompanyId();
        ServiceRequest sr = findInTenant(requestId);
        User currentUser = securityUtil.getCurrentUser();

        // Default visibility is role-aware: clients post PUBLIC comments (visible to staff),
        // employees/admins default to INTERNAL (not visible to client by default).
        CommentVisibility defaultVisibility = isClientRole(currentUser)
                ? CommentVisibility.CLIENT : CommentVisibility.INTERNAL;

        RequestComment comment = RequestComment.builder()
            .content(request.getContent())
            .visibility(request.getVisibility() != null
                ? request.getVisibility() : defaultVisibility)
            .attachmentUrl(request.getAttachmentUrl())
            .serviceRequest(sr)
            .company(companyRef(companyId))
            .author(currentUser)
            .build();

        commentRepository.save(comment);
        RequestCommentResponse response = ServiceRequestMapper.toCommentResponse(comment);

        // Notify whoever's on the OTHER side of the conversation, not the author -
        // this used to always notify the client, even when the client themself was
        // the one writing the comment.
        try {
            if (isClientRole(currentUser)) {
                Employee assigned = sr.getAssignedEmployee();
                if (assigned != null && assigned.getUser() != null) {
                    Long agentId = assigned.getUser().getId();
                    String clientLabel = sr.getClient() != null && sr.getClient().getClientCompanyName() != null
                        ? sr.getClient().getClientCompanyName() : currentUser.getFirstName();
                    notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                        NotificationType.REQUEST_UPDATED, "New Message",
                        clientLabel + " sent a message on \"" + sr.getTitle() + "\".",
                        agentId, companyId, requestId
                    ));
                    pushChatMessage(requestId, agentId, response);
                } else {
                    // Not assigned yet - nobody specific to hand it to, so alert
                    // whoever can pick it up, same as on initial submission.
                    List<Long> staffRecipients = notifyAssignableStaff(companyId, NotificationType.REQUEST_UPDATED,
                        "New Message", "A client sent a message on \"" + sr.getTitle() + "\".", requestId);
                    staffRecipients.forEach(id -> pushChatMessage(requestId, id, response));
                }
            } else if (comment.getVisibility() == CommentVisibility.CLIENT
                    && sr.getClient() != null && sr.getClient().getUser() != null) {
                Long clientUserId = sr.getClient().getUser().getId();
                notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                    NotificationType.REQUEST_UPDATED, "New Message",
                    "A new update has been added to your request \"" + sr.getTitle() + "\".",
                    clientUserId, companyId, requestId
                ));
                pushChatMessage(requestId, clientUserId, response);
            }
        } catch (Exception ex) {
            log.warn("Message notification failed for request {}: {}", requestId, ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestCommentResponse> getComments(Long requestId, Pageable pageable) {
        findInTenant(requestId);
        // CLIENT callers must never see INTERNAL (staff-only) notes on their own
        // request - guardAccess() in findInTenant() already confirms this is their
        // request, but visibility within that thread still needs enforcing here.
        User currentUser = securityUtil.getCurrentUser();
        Page<RequestComment> comments = isClientRole(currentUser)
            ? commentRepository.findByServiceRequestIdAndVisibilityOrderByCreatedAtDesc(
                requestId, CommentVisibility.CLIENT, pageable)
            : commentRepository.findByServiceRequestIdOrderByCreatedAtDesc(requestId, pageable);
        return comments.map(ServiceRequestMapper::toCommentResponse);
    }

    // ── Status history ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RequestStatusHistoryResponse> getStatusHistory(Long requestId) {
        findInTenant(requestId);
        return historyRepository
            .findByServiceRequestIdOrderByChangedAtAsc(requestId)
            .stream().map(ServiceRequestMapper::toHistoryResponse).toList();
    }

    @Override
    @Transactional
    public ServiceRequestResponse advanceStage(Long id) {
        ServiceRequest sr = findInTenant(id);
        guardNotClosed(sr);

        List<WorkflowStage> stages = loadWorkflowStages(sr);
        int current = sr.getCurrentStage() != null ? sr.getCurrentStage() : 0;
        if (current >= stages.size()) {
            throw new BadRequestException("Request is already at the final workflow stage");
        }

        WorkflowStage next = stages.get(current); // currentStage counts completed stages
        if (Boolean.TRUE.equals(next.getRequiresApproval())
            && !stageApprovalRepository.existsByServiceRequestIdAndWorkflowStageIdAndStatus(
                sr.getId(), next.getId(), ApprovalStatus.APPROVED)) {

            ensurePendingStageApproval(sr, next);
            throw new BadRequestException("Stage \"" + next.getName()
                + "\" requires approval. An approval request is pending in the approvals queue.");
        }

        sr.setCurrentStage(current + 1);

        // Stage-level SLA: entering a stage with slaHours refreshes the deadline
        if (next.getSlaHours() != null && next.getSlaHours() > 0) {
            sr.setSlaHours(next.getSlaHours());
            sr.setSlaDeadline(LocalDateTime.now().plusHours(next.getSlaHours()));
            sr.setSlaBreach(false);
        }

        if (sr.getAssignedEmployee() != null && sr.getAssignedEmployee().getUser() != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_UPDATED,
                "Request moved to stage: " + next.getName(),
                "Request \"" + sr.getTitle() + "\" advanced to stage \"" + next.getName() + "\"",
                sr.getAssignedEmployee().getUser().getId(),
                requireCompanyId(),
                sr.getId()));
        }

        // Milestone billing: the stage that just completed asks for an
        // installment. This only notifies + posts a visible comment with the
        // amount - collecting it (Record Payment / a new partial invoice
        // line) stays a manual staff action, since agreedPrice may not be set
        // yet and we shouldn't silently invent an invoice.
        if (Boolean.TRUE.equals(next.getRequiresPayment())
                && sr.getClient() != null && sr.getClient().getUser() != null) {
            String amountText;
            if (sr.getAgreedPrice() != null && next.getPaymentPercent() != null) {
                java.math.BigDecimal milestoneAmount = sr.getAgreedPrice()
                    .multiply(java.math.BigDecimal.valueOf(next.getPaymentPercent()))
                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                amountText = next.getPaymentPercent() + "% (" + milestoneAmount + ")";
            } else if (next.getPaymentPercent() != null) {
                amountText = next.getPaymentPercent() + "%";
            } else {
                amountText = "the next installment";
            }

            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.PAYMENT_DUE,
                "Milestone payment due",
                "\"" + next.getName() + "\" is complete - " + amountText
                    + " payment is now due for \"" + sr.getTitle() + "\".",
                sr.getClient().getUser().getId(),
                requireCompanyId(),
                sr.getId()));

            commentRepository.save(RequestComment.builder()
                .content("Milestone reached: \"" + next.getName() + "\" is complete. "
                    + amountText + " payment is now due.")
                .visibility(CommentVisibility.CLIENT)
                .serviceRequest(sr)
                .company(sr.getCompany())
                .author(securityUtil.getCurrentUser())
                .build());
        }

        return toResponse(sr);
    }

    @Override
    @Transactional(readOnly = true)
    public StageProgressResponse getStageProgress(Long id) {
        ServiceRequest sr = findInTenant(id);
        List<WorkflowStage> stages = loadWorkflowStages(sr);
        int current = sr.getCurrentStage() != null ? sr.getCurrentStage() : 0;

        StageProgressResponse response = new StageProgressResponse();
        response.setServiceRequestId(sr.getId());
        response.setCurrentStage(current);
        response.setTotalStages(stages.size());
        response.setStages(stages.stream().map(stage -> {
            StageProgressResponse.StageItem item = new StageProgressResponse.StageItem();
            item.setStageId(stage.getId());
            item.setName(stage.getName());
            item.setStageOrder(stage.getStageOrder());
            item.setSlaHours(stage.getSlaHours());
            item.setRequiresApproval(stage.getRequiresApproval());
            item.setRequiresPayment(stage.getRequiresPayment());
            item.setPaymentPercent(stage.getPaymentPercent());
            int index = stages.indexOf(stage);
            item.setCompleted(index < current);
            item.setCurrent(index == current);
            if (Boolean.TRUE.equals(stage.getRequiresApproval())) {
                item.setApprovalStatus(resolveApprovalStatus(sr.getId(), stage.getId()));
            }
            return item;
        }).toList());
        return response;
    }

    private List<WorkflowStage> loadWorkflowStages(ServiceRequest sr) {
        if (sr.getCompanyService() == null || sr.getCompanyService().getWorkflowTemplate() == null) {
            throw new BadRequestException("This service has no workflow template configured");
        }
        List<WorkflowStage> stages = workflowStageRepository
            .findByWorkflowTemplateIdOrderByStageOrderAsc(sr.getCompanyService().getWorkflowTemplate().getId());
        if (stages.isEmpty()) {
            throw new BadRequestException("The workflow template has no stages configured");
        }
        return stages;
    }

    private String resolveApprovalStatus(Long serviceRequestId, Long stageId) {
        for (ApprovalStatus status : List.of(ApprovalStatus.APPROVED, ApprovalStatus.PENDING, ApprovalStatus.REJECTED)) {
            if (stageApprovalRepository.existsByServiceRequestIdAndWorkflowStageIdAndStatus(serviceRequestId, stageId, status)) {
                return status.name();
            }
        }
        return null;
    }

    // Reads + mapping run in aiTx.load(), which commits before the provider call
    // so no DB connection is held across it - see AiTransactionBoundary. Every
    // lazy association (client, assignedEmployee.user) is read in the callback.
    @Override
    public ServiceRequestResponse summarise(Long id) {
        PreparedPrompt<ServiceRequestResponse> prepared = aiTx.load(() -> {
            ServiceRequest sr = findInTenant(id);
            ServiceRequestResponse dto = toResponse(sr);

            long taskCount = taskRepository.countByServiceRequestId(sr.getId());
            long completedCount = taskRepository.countByServiceRequestIdAndStatus(sr.getId(), TaskStatus.COMPLETED);
            String recentComments = commentRepository
                .findByServiceRequestIdOrderByCreatedAtDesc(sr.getId(), PageRequest.of(0, 5))
                .stream()
                .map(RequestComment::getContent)
                .reduce((a, b) -> a + "\n- " + b)
                .map(joined -> "- " + joined)
                .orElse(null);

            return new PreparedPrompt<>(dto, ServiceRequestSummaryPromptBuilder.builder()
                .setTitle(sr.getTitle())
                .setDescription(sr.getDescription())
                .setStatus(sr.getStatus().name())
                .setPriority(sr.getPriority() != null ? sr.getPriority().name() : "NORMAL")
                .setClientName(sr.getClient() != null ? sr.getClient().getClientCompanyName() : null)
                .setAssignedEmployeeName(sr.getAssignedEmployee() != null && sr.getAssignedEmployee().getUser() != null
                    ? sr.getAssignedEmployee().getUser().getFullName() : null)
                .setTaskProgress(completedCount + " of " + taskCount + " tasks completed")
                .setSlaBreach(sr.isSlaBreach())
                .setRecentComments(recentComments)
                .build());
        });

        ServiceRequestResponse response = prepared.payload();
        response.setAiSummary(aiService.generateRaw(AiFeature.SERVICE_REQUEST_SUMMARY, prepared.prompt()));
        return response;
    }

    // Same aiTx.load() pattern as summarise() above - reads commit before the
    // provider call runs. Tagged under the same SERVICE_REQUEST_SUMMARY feature
    // for audit purposes since this is the same "ground a reply in the request's
    // real state" idea, just producing a reply instead of a status summary.
    @Override
    public ServiceRequestReplyDraftResponse draftReply(Long id, ServiceRequestReplyDraftRequest request) {
        PreparedPrompt<Void> prepared = aiTx.load(() -> {
            ServiceRequest sr = findInTenant(id);

            String recentComments = commentRepository
                .findByServiceRequestIdOrderByCreatedAtDesc(sr.getId(), PageRequest.of(0, 5))
                .stream()
                .map(RequestComment::getContent)
                .reduce((a, b) -> a + "\n- " + b)
                .map(joined -> "- " + joined)
                .orElse(null);

            return new PreparedPrompt<>(null, ServiceRequestReplyDraftPromptBuilder.builder()
                .setTitle(sr.getTitle())
                .setStatus(sr.getStatus().name())
                .setRecentComments(recentComments)
                .setRoughNotes(request.getRoughNotes())
                .build());
        });

        String reply = aiService.generateRaw(AiFeature.SERVICE_REQUEST_SUMMARY, prepared.prompt());
        return new ServiceRequestReplyDraftResponse(reply.trim());
    }

    /**
     * Notifies whoever can actually handle a request when there's no single assigned
     * employee to hand it to yet: the company owner plus any active employee whose
     * CustomRole holds SERVICE_REQUEST_ASSIGN. Used both when a request is first
     * submitted and when a client messages a request that hasn't been picked up yet.
     * Returns the notified user ids so callers can also push a live chat update to
     * the same people.
     */
    private List<Long> notifyAssignableStaff(Long companyId, NotificationType type, String title,
                                              String message, Long requestId) {
        List<Long> recipients = new ArrayList<>();
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Long ownerId = company.getOwner() != null ? company.getOwner().getId() : null;

        if (ownerId != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                type, title, message, ownerId, companyId, requestId));
            recipients.add(ownerId);
        }

        for (Employee employee : employeeRepository.findByCompanyIdAndActiveTrue(companyId)) {
            User employeeUser = employee.getUser();
            if (employeeUser == null || employeeUser.getId().equals(ownerId)
                    || employeeUser.getCustomRole() == null) {
                continue;
            }
            boolean canAssign = rolePermissionRepository.existsByCustomRoleIdAndPermission_Code(
                employeeUser.getCustomRole().getId(), PermissionCode.SERVICE_REQUEST_ASSIGN.name());
            if (canAssign) {
                notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                    type, title, message, employeeUser.getId(), companyId, requestId));
                recipients.add(employeeUser.getId());
            }
        }
        return recipients;
    }

    /**
     * Live-pushes a chat message to each recipient's personal queue so an open chat
     * screen updates instantly, instead of waiting on the 60s notification-bell poll.
     * Uses convertAndSendToUser (per-principal, not a public /topic) so a message on
     * one client's ticket can never be received by another client's open socket.
     */
    private void pushChatMessage(Long requestId, Long recipientUserId, RequestCommentResponse message) {
        try {
            messagingTemplate.convertAndSendToUser(
                recipientUserId.toString(), "/queue/service-requests/" + requestId + "/messages", message);
        } catch (Exception ex) {
            log.debug("Live chat push failed for user {} on request {}: {}", recipientUserId, requestId, ex.getMessage());
        }
    }

    private ServiceRequest findInTenant(Long id) {
        ServiceRequest sr = serviceRequestRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service request not found: " + id));
        guardAccess(sr);
        return sr;
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }

    private void guardNotClosed(ServiceRequest sr) {
        if (sr.isPermanentlyClosed())
            throw new BadRequestException("This request is permanently closed");
    }

    /**
     * Staff (COMPANY_OWNER/SYSTEM_ADMIN/EMPLOYEE) can access any request in their
     * company - listAll() already exposes the full company queue to EMPLOYEE, so
     * restricting individual-request access to "already assigned" would contradict
     * that and block staff from triaging unassigned requests. Only CLIENT is
     * restricted to requests they own.
     */
    private void guardAccess(ServiceRequest sr) {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null) return;

        String role = user.getRole().name();
        if (!role.equals("CLIENT")) return;

        boolean isOwner = sr.getClient() != null
                && sr.getClient().getUser() != null
                && sr.getClient().getUser().getId().equals(user.getId());

        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to access this service request");
        }
    }

    private void recordStatusChange(ServiceRequest sr, ServiceRequestStatus oldStatus,
                                     ServiceRequestStatus newStatus, String reason,
                                     User changedBy, Long companyId) {
        historyRepository.save(RequestStatusHistory.builder()
            .serviceRequest(sr)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .reason(reason)
            .changedBy(changedBy)
            .companyId(companyId)
            .build());
    }

    private void notifyClientOnStatusChange(ServiceRequest sr,
                                             ServiceRequestStatus newStatus) {
        if (sr.getClient() == null || sr.getClient().getUser() == null) return;
        NotificationType type = switch (newStatus) {
            case COMPLETED      -> NotificationType.COMPLETED;
            case REJECTED       -> NotificationType.REJECTED;
            case CANCELLED      -> NotificationType.CANCELLED;
            case IN_PROGRESS    -> NotificationType.REQUEST_UPDATED;
            case WAITING_CLIENT -> NotificationType.REQUEST_UPDATED;
            default             -> null;
        };
        if (type == null) return;
        notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
            type, "Request Update",
            "Your request \"" + sr.getTitle() + "\" is now "
                + newStatus.name().replace('_', ' ') + ".",
            sr.getClient().getUser().getId(),
            sr.getCompany().getId(),
            sr.getId()
        ));

        if (newStatus == ServiceRequestStatus.COMPLETED) {
            try {
                Company fullCompany = companyRepository.findById(sr.getCompany().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                EmailBranding.Data branding = emailBranding.from(fullCompany);
                emailService.sendTicketResolvedEmail(sr.getClient().getUser().getEmail(), sr.getClient().getUser().getFirstName(), sr.getTitle(), branding);
            } catch (Exception ex) {
                log.warn("Ticket resolved email failed for client {}: {}", sr.getClient().getUser().getEmail(), ex.getMessage());
            }
        }
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr) {
        long taskCount = taskRepository.countByServiceRequestId(sr.getId());
        long completedCount = taskRepository.countByServiceRequestIdAndStatus(
            sr.getId(), TaskStatus.COMPLETED);
        ServiceRequestResponse response = ServiceRequestMapper.toResponse(sr, taskCount, completedCount);
        if (sr.getFormDataJson() != null && !sr.getFormDataJson().isBlank()) {
            try {
                response.setFormData(objectMapper.readValue(
                    sr.getFormDataJson(), new TypeReference<Map<String, String>>() {}));
            } catch (Exception ex) {
                log.warn("Could not parse formDataJson for request {}: {}", sr.getId(), ex.getMessage());
            }
        }
        return response;
    }

    /**
     * Checks required dynamic form fields for the service are answered and
     * returns the answers serialized as JSON (null when there is nothing to store).
     * Answers are keyed by ServiceFormField id so renamed labels don't orphan data.
     */
    private String validateAndSerializeFormData(
            Long companyId, Long serviceId, Map<String, String> formData) {
        List<ServiceFormField> fields = serviceFormFieldRepository
            .findByCompanyIdAndServiceIdOrderBySortOrderAsc(companyId, serviceId)
            .stream()
            .filter(f -> !f.isDeleted())
            .toList();
        for (ServiceFormField field : fields) {
            String value = formData == null ? null : formData.get(String.valueOf(field.getId()));
            
            // 1. Required Check
            if (field.isRequired() && (value == null || value.isBlank())) {
                throw new BadRequestException("'" + field.getLabel() + "' is required");
            }
            
            // 2. Format Checks (if value is provided)
            if (value != null && !value.isBlank()) {
                FormFieldType type = field.getFieldType();
                if (type == FormFieldType.NUMBER) {
                    try {
                        new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        throw new BadRequestException("'" + field.getLabel() + "' must be a valid number");
                    }
                } else if (type == FormFieldType.DATE) {
                    try {
                        java.time.LocalDate.parse(value);
                    } catch (java.time.format.DateTimeParseException e) {
                        throw new BadRequestException("'" + field.getLabel() + "' must be a valid date (YYYY-MM-DD)");
                    }
                } else if (type == FormFieldType.EMAIL) {
                    if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        throw new BadRequestException("'" + field.getLabel() + "' must be a valid email address");
                    }
                } else if (type == FormFieldType.PHONE) {
                    if (!value.matches("^\\+?[0-9\\s-]{7,15}$")) {
                        throw new BadRequestException("'" + field.getLabel() + "' must be a valid phone number");
                    }
                } else if (type == FormFieldType.FILE_UPLOAD) {
                    if (!value.matches("^https?://.*$")) {
                        throw new BadRequestException("'" + field.getLabel() + "' must be a valid file URL (http:// or https://)");
                    }
                }
            }
        }
        if (formData == null || formData.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid form data");
        }
    }

    @Override
    @Transactional
    public ServiceRequestResponse submitQuotation(Long id, SubmitQuotationRequest request) {
        Long companyId = requireCompanyId();
        ServiceRequest sr = findInTenant(id);

        sr.submitQuotation(request.getAmount(), request.getCurrency(), request.getNotes(), request.getValidUntil());
        sr = serviceRequestRepository.save(sr);

        if (sr.getClient() != null && sr.getClient().getUser() != null) {
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                NotificationType.REQUEST_UPDATED,
                "Quotation Ready",
                "A quotation of " + sr.getQuotationAmount() + " " + sr.getQuotationCurrency()
                    + " is ready for your review on \"" + sr.getTitle() + "\".",
                sr.getClient().getUser().getId(), companyId, id
            ));
        }

        return toResponse(sr);
    }

    @Override
    @Transactional
    public ServiceRequestResponse acceptQuotation(Long id) {
        Long companyId = requireCompanyId();
        ServiceRequest sr = findInTenant(id);

        if (sr.getQuotationStatus() != com.businessos.enums.QuotationStatus.PENDING) {
            throw new BadRequestException("Only pending quotations can be accepted.");
        }
        if (sr.getQuotationValidUntil() != null && LocalDateTime.now().isAfter(sr.getQuotationValidUntil())) {
            sr.setQuotationStatus(com.businessos.enums.QuotationStatus.EXPIRED);
            serviceRequestRepository.save(sr);
            throw new BadRequestException("This quotation expired on " + sr.getQuotationValidUntil()
                + " and can no longer be accepted");
        }

        sr.acceptQuotation();
        sr = serviceRequestRepository.save(sr);

        notifyEmployeeOnQuotationDecision(sr, companyId, "accepted");

        ServiceRequestResponse response = toResponse(sr);

        if (sr.getAgreedPrice() != null && sr.getAgreedPrice().compareTo(BigDecimal.ZERO) > 0) {
            ClientInvoiceItemRequest item = ClientInvoiceItemRequest.builder()
                .description("Service Request (Quotation Accepted): " + sr.getTitle())
                .quantity(new BigDecimal("1"))
                .unitPrice(sr.getAgreedPrice())
                .build();

            ClientInvoiceRequest invoiceRequest = ClientInvoiceRequest.builder()
                .clientId(sr.getClient().getId())
                .serviceRequestId(sr.getId())
                .invoiceDate(java.time.LocalDate.now())
                .dueDate(java.time.LocalDate.now().plusDays(3))
                .notes("Invoice for Service Request: " + sr.getTitle())
                .items(List.of(item))
                .build();

            ClientInvoiceResponse invoiceResponse = invoiceService.createForServiceRequest(companyId, invoiceRequest);
            sr.setInvoiceId(invoiceResponse.getId());
            serviceRequestRepository.save(sr);
            response.setInvoiceId(invoiceResponse.getId());
            
            invoiceService.sendInvoiceForServiceRequest(invoiceResponse.getId());
        }
        
        return response;
    }

    @Override
    @Transactional
    public ServiceRequestResponse rejectQuotation(Long id, RejectQuotationRequest request) {
        Long companyId = requireCompanyId();
        ServiceRequest sr = findInTenant(id);

        if (sr.getQuotationStatus() != com.businessos.enums.QuotationStatus.PENDING) {
            throw new BadRequestException("Only pending quotations can be rejected.");
        }

        sr.rejectQuotation(request.getReason());
        sr = serviceRequestRepository.save(sr);

        notifyEmployeeOnQuotationDecision(sr, companyId, "rejected");

        return toResponse(sr);
    }

    private void notifyEmployeeOnQuotationDecision(ServiceRequest sr, Long companyId, String decision) {
        if (sr.getAssignedEmployee() == null || sr.getAssignedEmployee().getUser() == null) return;
        notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
            NotificationType.REQUEST_UPDATED,
            "Quotation " + decision.substring(0, 1).toUpperCase() + decision.substring(1),
            "The client " + decision + " the quotation for \"" + sr.getTitle() + "\".",
            sr.getAssignedEmployee().getUser().getId(), companyId, sr.getId()
        ));
    }

    /**
     * Creates the pending StageApproval that advanceStage() promises in its error
     * message, in a transaction of its own. advanceStage() throws right after
     * calling this to send the caller to the approvals queue - if the save lived
     * in that same @Transactional method, Spring's default rollback-on-exception
     * would undo it along with everything else, and the queue would stay empty
     * forever despite the message saying otherwise. REQUIRES_NEW via
     * TransactionTemplate (not a second @Transactional method on this class) is
     * required here specifically because self-invocation doesn't go through the
     * Spring proxy that propagation annotations rely on.
     */
    private void ensurePendingStageApproval(ServiceRequest sr, WorkflowStage next) {
        org.springframework.transaction.support.TransactionTemplate requiresNew =
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(
            org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.executeWithoutResult(status -> {
            boolean alreadyPending = stageApprovalRepository
                .existsByServiceRequestIdAndWorkflowStageIdAndStatus(sr.getId(), next.getId(), ApprovalStatus.PENDING);
            if (!alreadyPending) {
                stageApprovalRepository.save(StageApproval.builder()
                    .serviceRequest(sr)
                    .workflowStage(next)
                    .approverRole(next.getAssigneeRole())
                    .requestedBy(securityUtil.getCurrentUser())
                    .company(sr.getCompany())
                    .build());
            }
        });
    }

    /**
     * Blocks ordering a service before a mandatory prerequisite has been
     * completed - e.g. Trade License Registration requires a completed
     * Company Incorporation first. "Completed" means the client has at least
     * one COMPLETED request for that prerequisite service with this company;
     * an in-progress or rejected attempt doesn't satisfy it.
     */
    private void validatePrerequisites(Long companyId, Long clientId, CompanyService service) {
        List<com.businessos.modules.servicedesk.companyservice.ServicePrerequisite> prerequisites =
            servicePrerequisiteRepository.findByServiceIdOrderByIdAsc(service.getId());

        for (com.businessos.modules.servicedesk.companyservice.ServicePrerequisite prereq : prerequisites) {
            if (!prereq.isMandatory()) continue;

            CompanyService prereqService = prereq.getPrerequisiteService();
            boolean satisfied = serviceRequestRepository.existsByCompanyIdAndClientIdAndCompanyServiceIdAndStatus(
                companyId, clientId, prereqService.getId(), ServiceRequestStatus.COMPLETED);

            if (!satisfied) {
                String message = prereq.getMessage() != null && !prereq.getMessage().isBlank()
                    ? prereq.getMessage()
                    : "'" + service.getName() + "' requires a completed '" + prereqService.getName() + "' first";
                throw new BadRequestException(message);
            }
        }
    }

    private void validateRequiredDocuments(ServiceRequest sr) {
        CompanyService service = sr.getCompanyService();
        if (service == null) return;
        
        Long companyId = sr.getCompany().getId();
        
        List<com.businessos.modules.servicedesk.document.RequiredDocument> mandatoryDocs = requiredDocumentRepository
            .findByCompanyIdAndServiceIdOrderBySortOrderAsc(companyId, service.getId())
            .stream()
            .filter(com.businessos.modules.servicedesk.document.RequiredDocument::isMandatory)
            .toList();
            
        if (mandatoryDocs.isEmpty()) return;
        
        List<com.businessos.modules.servicedesk.document.Document> uploadedDocs = documentRepository
            .findByServiceRequestIdOrderByCreatedAtDesc(sr.getId());
        
        for (com.businessos.modules.servicedesk.document.RequiredDocument reqDoc : mandatoryDocs) {
            boolean uploaded = uploadedDocs.stream()
                .anyMatch(doc -> doc.getLabel() != null && doc.getLabel().equalsIgnoreCase(reqDoc.getDocName().trim()));
            if (!uploaded) {
                throw new BadRequestException("Mandatory document '" + reqDoc.getDocName() + "' is missing");
            }
        }
    }

    private boolean isClientRole(User user) {
        return user != null && user.getRole() != null
                && user.getRole().name().equals("CLIENT");
    }
}
