package com.businessos.modules.support.message;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.enums.NotificationType;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.support.agent.SupportAgent;
import com.businessos.modules.support.agent.SupportAgentRepository;
import com.businessos.modules.support.agent.SupportAgentStatus;
import com.businessos.modules.support.ticket.SupportTicket;
import com.businessos.modules.support.ticket.SupportTicketRepository;
import com.businessos.modules.support.ticket.TicketType;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportMessageServiceImpl implements SupportMessageService {

    private final SupportMessageRepository messageRepository;
    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SupportAgentRepository supportAgentRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public SupportMessageResponse create(SupportMessageRequest request) {
        // findById on SupportTicket is tenant-filtered for tenant callers (see its
        // @Filter) - a company can't already reach another company's ticket here.
        // Platform staff (SUPPORT_AGENT/SUPPORT_MANAGER) bypass that filter by design,
        // since they need to work any company's tickets.
        SupportTicket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // The sender must be the authenticated caller, never client-supplied - trusting
        // request.getSentByUserId() let any caller post a message as an arbitrary user.
        User sentBy = securityUtil.getCurrentUser();
        if (sentBy == null) {
            throw new ForbiddenException("Not authenticated");
        }

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .sentBy(sentBy)
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .isInternal(request.isInternal())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentFileName(request.getAttachmentFileName())
                .isResolution(request.isResolution())
                .build();

        message = messageRepository.save(message);
        SupportMessageResponse response = SupportMessageMapper.toResponse(message);

        // Internal notes are staff-only by definition - never alert the other side.
        if (!message.isInternal()) {
            notifyOtherParty(ticket, sentBy, response);
        }

        return response;
    }

    /**
     * CLIENT posts a message on their own CUSTOMER_SUPPORT ticket. isInternal is
     * never taken from the request - a client-authored message is always external.
     */
    @Override
    @Transactional
    public SupportMessageResponse createForClient(SupportMessageRequest request) {
        Client client = resolveClientForCurrentUser();
        SupportTicket ticket = ticketRepository
                .findByIdAndClientIdAndCompanyId(request.getTicketId(), client.getId(), client.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .sentBy(client.getUser())
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .isInternal(false)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentFileName(request.getAttachmentFileName())
                .build();

        message = messageRepository.save(message);
        SupportMessageResponse response = SupportMessageMapper.toResponse(message);
        notifyOtherParty(ticket, client.getUser(), response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getClientMessages(Long ticketId) {
        Client client = resolveClientForCurrentUser();
        ticketRepository.findByIdAndClientIdAndCompanyId(ticketId, client.getId(), client.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        // A client only ever sees external messages - isInternal notes are staff-only.
        return messageRepository.findByTicketIdAndIsInternalFalse(ticketId)
                .stream()
                .map(SupportMessageMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Client resolveClientForCurrentUser() {
        User current = securityUtil.getCurrentUser();
        if (current == null) {
            throw new BadRequestException("Not authenticated");
        }
        return clientRepository.findByUserId(current.getId())
                .orElseThrow(() -> new BadRequestException("No client profile linked to this account"));
    }

    /**
     * Branches on the ticket's own type, not just the sender's tenant-ness -
     * PLATFORM_SUPPORT and CUSTOMER_SUPPORT tickets both have a tenant-side
     * sender (COMPANY_OWNER/EMPLOYEE), so that alone can't tell them apart.
     * Getting this wrong would notify BusinessOS platform SupportAgents about
     * a client's internal support message, or vice versa.
     */
    private void notifyOtherParty(SupportTicket ticket, User sender, SupportMessageResponse response) {
        try {
            if (ticket.getTicketType() == TicketType.CUSTOMER_SUPPORT) {
                notifyOnCustomerSupportTicket(ticket, sender, response);
            } else {
                notifyOnPlatformSupportTicket(ticket, sender, response);
            }
        } catch (Exception ex) {
            log.warn("Support message notification failed for ticket {}: {}", ticket.getId(), ex.getMessage());
        }
    }

    /**
     * Company -> platform: notify the assigned agent if the ticket has one, otherwise
     * broadcast to agents currently accepting tickets (falling back to SUPPORT_MANAGER
     * if none are available right now) - same "nobody's picked this up yet" pattern as
     * ServiceRequestServiceImpl.notifyAssignableStaff.
     * Platform -> company: notify whoever opened the ticket.
     * Also live-pushes the message to each recipient's personal queue so an open chat
     * screen updates instantly, same mechanism as ServiceRequestServiceImpl.pushChatMessage.
     */
    private void notifyOnPlatformSupportTicket(SupportTicket ticket, User sender, SupportMessageResponse response) {
        String actionUrl = "/support/tickets/" + ticket.getId();
        List<Long> recipients = new ArrayList<>();

        if (sender.isTenantUser()) {
            if (ticket.getAssignedToAgent() != null && ticket.getAssignedToAgent().getUser() != null) {
                recipients.add(ticket.getAssignedToAgent().getUser().getId());
            } else {
                for (SupportAgent agent : supportAgentRepository
                        .findByStatusAndAcceptingTicketsTrue(SupportAgentStatus.ACTIVE)) {
                    if (agent.getUser() != null) {
                        recipients.add(agent.getUser().getId());
                    }
                }
                if (recipients.isEmpty()) {
                    userRepository.findByRoleIn(List.of(Role.SUPPORT_MANAGER), Pageable.unpaged())
                            .forEach(u -> recipients.add(u.getId()));
                }
            }
            String message = "New message on ticket " + ticket.getTicketNumber()
                    + (ticket.getCompany() != null ? " from " + ticket.getCompany().getCompanyName() : "") + ".";
            for (Long recipientId : recipients) {
                notificationService.send(CreateNotificationRequest.of(
                        NotificationType.GENERAL, "New Support Message", message,
                        actionUrl, recipientId, ticket.getCompanyId()));
                pushChatMessage(ticket.getId(), recipientId, response);
            }
        } else if (ticket.getCreatedBy() != null) {
            Long recipientId = ticket.getCreatedBy().getId();
            String message = "Support replied on ticket " + ticket.getTicketNumber() + ".";
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.GENERAL, "New Support Message", message,
                    actionUrl, recipientId, ticket.getCompanyId()));
            pushChatMessage(ticket.getId(), recipientId, response);
        }
    }

    /**
     * Client -> company: notify the assigned Employee if the ticket has one,
     * otherwise the company owner - there's no "unassigned ticket pool" broadcast
     * for CUSTOMER_SUPPORT yet, unlike the platform SupportAgent pool above
     * (createForClient() in SupportTicketServiceImpl never sets assignedEmployee).
     * Company -> client: notify whoever the ticket belongs to.
     */
    private void notifyOnCustomerSupportTicket(SupportTicket ticket, User sender, SupportMessageResponse response) {
        String actionUrl = "/client/tickets/" + ticket.getId();
        boolean fromClient = ticket.getClient() != null && ticket.getClient().getUser() != null
                && ticket.getClient().getUser().getId().equals(sender.getId());

        if (fromClient) {
            Long recipientId = ticket.getAssignedEmployee() != null && ticket.getAssignedEmployee().getUser() != null
                    ? ticket.getAssignedEmployee().getUser().getId()
                    : ticket.getCompany() != null && ticket.getCompany().getOwner() != null
                        ? ticket.getCompany().getOwner().getId()
                        : null;
            if (recipientId == null) return;
            String message = "New message on ticket " + ticket.getTicketNumber()
                    + (ticket.getClient() != null ? " from " + ticket.getClient().getClientCompanyName() : "") + ".";
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.GENERAL, "New Support Message", message,
                    actionUrl, recipientId, ticket.getCompanyId()));
            pushChatMessage(ticket.getId(), recipientId, response);
        } else if (ticket.getClient() != null && ticket.getClient().getUser() != null) {
            Long recipientId = ticket.getClient().getUser().getId();
            String message = "Support replied on ticket " + ticket.getTicketNumber() + ".";
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.GENERAL, "New Support Message", message,
                    actionUrl, recipientId, ticket.getCompanyId()));
            pushChatMessage(ticket.getId(), recipientId, response);
        }
    }

    private void pushChatMessage(Long ticketId, Long recipientUserId, SupportMessageResponse message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    recipientUserId.toString(), "/queue/support-tickets/" + ticketId + "/messages", message);
        } catch (Exception ex) {
            log.debug("Live chat push failed for user {} on ticket {}: {}", recipientUserId, ticketId, ex.getMessage());
        }
    }

    /**
     * SupportMessage has no tenantFilter of its own (only SupportTicket does), and
     * update()/delete() fetch by raw id - without this, an EMPLOYEE at one company
     * could edit/delete a message belonging to a different company's ticket.
     */
    private void assertTenantAccess(SupportMessage message) {
        User current = securityUtil.getCurrentUser();
        if (current == null || !current.isTenantUser()) {
            return; // platform staff (SUPPORT_AGENT/SUPPORT_MANAGER) work across companies
        }
        Long companyId = securityUtil.getCurrentCompanyId();
        Long ticketCompanyId = message.getTicket() != null ? message.getTicket().getCompanyId() : null;
        if (companyId == null || !companyId.equals(ticketCompanyId)) {
            throw new ForbiddenException("You do not have permission to access this message");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SupportMessageResponse getById(Long id) {
        SupportMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        assertTenantAccess(message);
        return SupportMessageMapper.toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportMessageResponse> getByTicket(Long ticketId, Pageable pageable) {
        checkTenantPermission();
        assertTicketExists(ticketId);
        return messageRepository.findByTicketId(ticketId, pageable)
                .map(SupportMessageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getExternalMessages(Long ticketId) {
        checkTenantPermission();
        assertTicketExists(ticketId);
        return messageRepository.findByTicketIdAndIsInternalFalse(ticketId)
                .stream()
                .map(SupportMessageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getInternalNotes(Long ticketId) {
        checkTenantPermission();
        assertTicketExists(ticketId);
        return messageRepository.findByTicketIdAndIsInternalTrue(ticketId)
                .stream()
                .map(SupportMessageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupportMessageResponse update(Long id, SupportMessageRequest request) {
        SupportMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        assertTenantAccess(message);

        message.setMessage(request.getMessage());
        message.setAttachmentUrl(request.getAttachmentUrl());

        message = messageRepository.save(message);
        return SupportMessageMapper.toResponse(message);
    }

    @Override
    @Transactional
    public SupportMessageResponse delete(Long id) {
        SupportMessage message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        assertTenantAccess(message);
        message.softDelete();
        messageRepository.save(message);
        return SupportMessageMapper.toResponse(message);
    }

    /** Relies on SupportTicket's own tenantFilter to 404 a ticket the caller can't reach. */
    private void assertTicketExists(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found: " + ticketId);
        }
    }

    // Both endpoints also allow SUPPORT_AGENT/SUPPORT_MANAGER (platform staff with no
    // CustomRole) per their @PreAuthorize - only gate the tenant caller branch here.
    private void checkTenantPermission() {
        com.businessos.auth.user.User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            authorizationService.checkPermission(PermissionCode.SUPPORT_MESSAGE_VIEW);
        }
    }
}
