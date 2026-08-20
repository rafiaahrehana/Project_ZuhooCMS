package com.businessos.modules.support.ticket;

import com.businessos.modules.support.agent.SupportAgent;
import com.businessos.modules.support.agent.SupportAgentRepository;
import com.businessos.modules.support.agent.SupportAgentStatus;
import com.businessos.shared.audit.AuditLog;
import com.businessos.modules.support.audit.SupportAuditLogRepository;
import com.businessos.modules.support.category.SupportCategory;
import com.businessos.modules.support.category.SupportCategoryRepository;
import com.businessos.modules.support.sla.SLAPolicy;
import com.businessos.modules.support.sla.SLAPolicyRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportCategoryRepository categoryRepository;
    private final SupportAgentRepository agentRepository;
    private final SLAPolicyRepository slaPolicyRepository;
    private final SupportAuditLogRepository auditRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    @Override
    @Transactional
    public SupportTicketResponse create(SupportTicketRequest request) {
        Long companyId = securityUtil.getCurrentCompanyId();
        Long currentUserId = securityUtil.getCurrentUser().getId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        User createdBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        String ticketNumber = generateTicketNumber();

        // Get SLA policy based on priority
        SLAPolicy slaPolicy = slaPolicyRepository.findByApplicablePriorityAndActiveTrue(request.getPriority())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstResponseDeadline = null;
        LocalDateTime resolutionDeadline = null;

        if (slaPolicy != null) {
            firstResponseDeadline = now.plusHours(slaPolicy.getFirstResponseTimeHours());
            resolutionDeadline = now.plusHours(slaPolicy.getResolutionTimeHours());
        }

        SupportTicket ticket = SupportTicket.builder()
                .companyId(companyId)
                .ticketNumber(ticketNumber)
                .company(company)
                .createdBy(createdBy)
                .title(request.getTitle())
                .description(request.getDescription())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentFileName(request.getAttachmentFileName())
                .category(category)
                .status(TicketStatus.NEW)
                .priority(request.getPriority())
                .source(request.getSource())
                .firstResponseDeadline(firstResponseDeadline)
                .resolutionDeadline(resolutionDeadline)
                .build();

        ticket = ticketRepository.save(ticket);

        // Log audit
        logAudit(companyId, currentUserId, "CREATE_TICKET", ticket.getId(), "SupportTicket",
                "Ticket created: " + ticketNumber, null);

        return SupportTicketMapper.toResponse(ticket);
    }

    /**
     * CLIENT raises a CUSTOMER_SUPPORT ticket against their own client-company -
     * distinct from create() above, which is PLATFORM_SUPPORT only (tenant staff
     * reporting an issue with BusinessOS itself, resolved by SupportAgent). A
     * CUSTOMER_SUPPORT ticket has no SupportAgent involved at all; it stays
     * unassigned until a staff member (COMPANY_OWNER/EMPLOYEE) picks it up -
     * assigning a CUSTOMER_SUPPORT ticket to a specific Employee has no endpoint
     * yet (assignToAgent() below only assigns the platform SupportAgent kind);
     * staff can still see, message, and resolve/close it in the meantime.
     */
    @Override
    @Transactional
    public SupportTicketResponse createForClient(SupportTicketRequest request) {
        Client client = resolveClientForCurrentUser();
        Long companyId = client.getCompany().getId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String ticketNumber = generateTicketNumber();

        SLAPolicy slaPolicy = slaPolicyRepository.findByApplicablePriorityAndActiveTrue(request.getPriority())
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstResponseDeadline = slaPolicy != null ? now.plusHours(slaPolicy.getFirstResponseTimeHours()) : null;
        LocalDateTime resolutionDeadline = slaPolicy != null ? now.plusHours(slaPolicy.getResolutionTimeHours()) : null;

        SupportTicket ticket = SupportTicket.builder()
                .companyId(companyId)
                .ticketNumber(ticketNumber)
                .ticketType(TicketType.CUSTOMER_SUPPORT)
                .company(company)
                .createdBy(client.getUser())
                .client(client)
                .title(request.getTitle())
                .description(request.getDescription())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentFileName(request.getAttachmentFileName())
                .status(TicketStatus.NEW)
                .priority(request.getPriority())
                .source(request.getSource())
                .firstResponseDeadline(firstResponseDeadline)
                .resolutionDeadline(resolutionDeadline)
                .build();

        ticket = ticketRepository.save(ticket);

        logAudit(companyId, client.getUser().getId(), "CREATE_TICKET", ticket.getId(), "SupportTicket",
                "Customer support ticket created: " + ticketNumber, null);

        return SupportTicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyClientTickets(Pageable pageable) {
        Client client = resolveClientForCurrentUser();
        return ticketRepository.findByClientIdAndCompanyId(client.getId(), client.getCompany().getId(), pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getClientTicketById(Long id) {
        Client client = resolveClientForCurrentUser();
        SupportTicket ticket = ticketRepository
                .findByIdAndClientIdAndCompanyId(id, client.getId(), client.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return SupportTicketMapper.toResponse(ticket);
    }

    /** Mirrors ServicePackageServiceImpl's client-resolution pattern. */
    private Client resolveClientForCurrentUser() {
        User current = securityUtil.getCurrentUser();
        if (current == null) {
            throw new BadRequestException("Not authenticated");
        }
        return clientRepository.findByUserId(current.getId())
                .orElseThrow(() -> new BadRequestException("No client profile linked to this account"));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getById(Long id) {
        return SupportTicketMapper.toResponse(findTicketForCaller(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getByTicketNumber(String number) {
        SupportTicket ticket = ticketRepository.findByTicketNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        requireCallerOwns(ticket);
        return SupportTicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAll(Pageable pageable) {
        // Platform support staff (SUPPORT_MANAGER/SUPER_ADMIN/SYSTEM_ADMIN) triage
        // tickets across every company - only a tenant caller (COMPANY_OWNER) gets
        // scoped to their own company. findAll() here was previously unscoped for
        // everyone, letting a company owner see every other tenant's tickets.
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            // Fine-grained permission only applies to tenant users - platform staff
            // (SUPPORT_MANAGER etc.) have no CustomRole, so checkPermission() would
            // always deny them; their existing role-based @PreAuthorize already gates
            // this endpoint for that branch.
            authorizationService.checkPermission(PermissionCode.TICKET_VIEW);
            // This is "my company's tickets to BusinessOS" (Platform Tickets / Direct
            // Messages) - it predates ticketType existing at all, so it never filtered
            // by it and ended up mixing in CUSTOMER_SUPPORT tickets (a client
            // messaging this company) too. getClientTicketsForCompany() below is the
            // counterpart for those.
            return ticketRepository.findByCompanyIdAndTicketType(
                    securityUtil.getCurrentCompanyId(), TicketType.PLATFORM_SUPPORT, pageable)
                    .map(SupportTicketMapper::toResponse);
        }
        return ticketRepository.findAll(pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getByCompany(Long companyId, Pageable pageable) {
        return ticketRepository.findByCompanyId(companyId, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getByStatus(TicketStatus status, Pageable pageable) {
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            authorizationService.checkPermission(PermissionCode.TICKET_VIEW);
            return ticketRepository.findByCompanyIdAndTicketTypeAndStatus(
                    securityUtil.getCurrentCompanyId(), TicketType.PLATFORM_SUPPORT, status, pageable)
                    .map(SupportTicketMapper::toResponse);
        }
        return ticketRepository.findByStatus(status, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    /**
     * Staff-facing counterpart to getAll() above: this company's own clients'
     * CUSTOMER_SUPPORT tickets, not tickets to BusinessOS. Any COMPANY_OWNER/
     * EMPLOYEE with TICKET_VIEW can see every client's ticket in their company,
     * same "whole team can see the shared inbox" model getAll() already uses -
     * assignedEmployee is who's actively handling it, not a visibility filter.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getClientTicketsForCompany(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.TICKET_VIEW);
        return ticketRepository.findByCompanyIdAndTicketType(
                securityUtil.getCurrentCompanyId(), TicketType.CUSTOMER_SUPPORT, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getClientTicketsForCompanyByStatus(TicketStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.TICKET_VIEW);
        return ticketRepository.findByCompanyIdAndTicketTypeAndStatus(
                securityUtil.getCurrentCompanyId(), TicketType.CUSTOMER_SUPPORT, status, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAssignedToMe(Long agentId, Pageable pageable) {
        if (agentId == null) {
            return Page.empty(pageable);
        }
        return ticketRepository.findByAssignedToAgentId(agentId, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyTickets(Long userId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.TICKET_VIEW);
        Long targetUserId = userId != null ? userId : securityUtil.getCurrentUser().getId();
        return ticketRepository.findByCreatedById(targetUserId, pageable)
                .map(SupportTicketMapper::toResponse);
    }

    @Override
    @Transactional
    public void assignToAgent(Long ticketId, Long agentId) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        SupportAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        requireActiveAgent(agent);

        ticket.assignToAgent(agent);
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "ASSIGN", ticketId,
                "SupportTicket", "Assigned to " + agent.getUser().getFullName(), null);
    }

    @Override
    @Transactional
    public void reassignToAgent(Long ticketId, Long newAgentId, String reason) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        SupportAgent newAgent = agentRepository.findById(newAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        requireActiveAgent(newAgent);

        SupportAgent oldAgent = ticket.getAssignedToAgent();
        ticket.setAssignedToAgent(newAgent);
        ticket.setAssignedDate(LocalDateTime.now());
        ticketRepository.save(ticket);

        String oldAgentName = oldAgent != null ? oldAgent.getUser().getFullName() : "Unassigned";
        String description = String.format("Reassigned from %s to %s. Reason: %s",
                oldAgentName, newAgent.getUser().getFullName(), reason);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "REASSIGN", ticketId,
                "SupportTicket", description, null);
    }

    @Override
    @Transactional
    public void escalate(Long ticketId, String reason) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        ticket.setEscalationLevel(ticket.getEscalationLevel() + 1);
        ticket.setEscalatedDate(LocalDateTime.now());
        ticket.setEscalationReason(reason);
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "ESCALATE", ticketId,
                "SupportTicket", "Escalated to level " + ticket.getEscalationLevel() + ": " + reason, null);
    }

    @Override
    @Transactional
    public void recordFirstResponse(Long ticketId) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        ticket.recordFirstResponse();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "FIRST_RESPONSE", ticketId,
                "SupportTicket", "First response recorded", null);
    }

    @Override
    @Transactional
    public void resolve(Long ticketId, String resolutionNotes) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        Long currentUserId = securityUtil.getCurrentUser().getId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ticket.resolve(resolutionNotes, user.getFullName());
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), currentUserId, "RESOLVE", ticketId,
                "SupportTicket", "Ticket resolved", null);
    }

    @Override
    @Transactional
    public void close(Long ticketId) {
        SupportTicket ticket = findTicketForCaller(ticketId);
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new com.businessos.shared.exception.BadRequestException(
                    "Only a resolved ticket can be closed - resolve it first so the resolution is on record");
        }

        ticket.close();
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "CLOSE", ticketId,
                "SupportTicket", "Ticket closed", null);
    }

    @Override
    @Transactional
    public void reopen(Long ticketId, String reason) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        ticket.setStatus(TicketStatus.REOPENED);
        ticketRepository.save(ticket);

        logAudit(ticket.getCompanyId(), securityUtil.getCurrentUser().getId(), "REOPEN", ticketId,
                "SupportTicket", "Ticket reopened: " + reason, null);
    }

    @Override
    @Transactional
    public void recordSatisfaction(Long ticketId, int rating, String feedback) {
        SupportTicket ticket = findTicketForCaller(ticketId);

        ticket.setSatisfactionRating(rating);
        ticket.setSatisfactionFeedback(feedback);
        ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getSLABreachedTickets() {
        Long companyId = securityUtil.getCurrentCompanyId();
        // Scoped at the database for tenant callers - only a platform-staff
        // caller with no company context gets the cross-company scan.
        List<SupportTicket> tickets = companyId != null
                ? ticketRepository.findSLABreachedTickets(companyId, CLOSED_STATUSES, LocalDateTime.now())
                : ticketRepository.findSLABreachedTickets(CLOSED_STATUSES, LocalDateTime.now());
        return tickets.stream()
                .map(SupportTicketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getOpenCriticalTickets() {
        Long companyId = securityUtil.getCurrentCompanyId();
        List<SupportTicket> tickets = companyId != null
                ? ticketRepository.findOpenCriticalTickets(companyId)
                : ticketRepository.findOpenCriticalTickets();
        return tickets.stream()
                .map(SupportTicketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupportTicketResponse update(Long id, SupportTicketRequest request) {
        SupportTicket ticket = findTicketForCaller(id);

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());

        if (request.getCategoryId() != null) {
            SupportCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            ticket.setCategory(category);
        }

        ticket = ticketRepository.save(ticket);
        return SupportTicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SupportTicket ticket = findTicketForCaller(id);
        ticket.softDelete();
        ticketRepository.save(ticket);
    }

    /**
     * Tenant callers (COMPANY_OWNER/EMPLOYEE) are scoped strictly to their own
     * company; platform support staff (SUPPORT_AGENT/SUPPORT_MANAGER/SUPER_ADMIN/
     * SYSTEM_ADMIN) legitimately triage tickets across every company, same split
     * as getAll()/getByStatus() above.
     */
    // SupportAgent has no company_id (support staff are platform-wide, not
    // tenant-scoped) - "same company" doesn't apply the way it does elsewhere,
    // but assign/reassign never checked the target agent was even active or
    // under their own concurrent-ticket cap - maxConcurrentTickets was a
    // decorative field with no backing logic in assignment.
    private void requireActiveAgent(SupportAgent agent) {
        if (agent.getStatus() != SupportAgentStatus.ACTIVE) {
            throw new com.businessos.shared.exception.BadRequestException(
                    "Cannot assign to " + agent.getUser().getFullName() + " - they are not an active agent");
        }
        long openCount = ticketRepository.countByAssignedToAgentIdAndStatusNotIn(agent.getId(), CLOSED_STATUSES);
        if (openCount >= agent.getMaxConcurrentTickets()) {
            throw new com.businessos.shared.exception.BadRequestException(
                    "Cannot assign to " + agent.getUser().getFullName() + " - they already have "
                            + openCount + " open ticket(s), at their limit of " + agent.getMaxConcurrentTickets());
        }
    }

    private SupportTicket findTicketForCaller(Long id) {
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            return ticketRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        }
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    /** Used where the ticket was fetched by a non-tenant-scoped lookup (ticket number). */
    private void requireCallerOwns(SupportTicket ticket) {
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()
                && !java.util.Objects.equals(ticket.getCompanyId(), securityUtil.getCurrentCompanyId())) {
            throw new ResourceNotFoundException("Ticket not found");
        }
    }

    private String generateTicketNumber() {
        long count = ticketRepository.count() + 1;
        return String.format("TKT-%04d-%06d", LocalDate.now().getYear(), count);
    }

    private void logAudit(Long companyId, Long userId, String actionType, Long resourceId,
                          String resourceType, String description, String changes) {
        Company company = companyRepository.findById(companyId).orElse(null);
        AuditLog log = AuditLog.builder()
                .company(company)
                .performedBy(userRepository.findById(userId).orElse(null))
                .action(com.businessos.enums.AuditAction.valueOf(actionType))
                .entityId(resourceId)
                .entityType(com.businessos.enums.AuditEntityType.valueOf("SUPPORT_TICKET"))
                .oldValue(description)
                .newValue(changes)
                .build();
        auditRepository.save(log);
    }
}
