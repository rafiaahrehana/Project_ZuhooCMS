package com.businessos.modules.support.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupportTicketService {

    SupportTicketResponse create(SupportTicketRequest request);
    SupportTicketResponse getById(Long id);
    SupportTicketResponse getByTicketNumber(String number);
    Page<SupportTicketResponse> getAll(Pageable pageable);
    Page<SupportTicketResponse> getByCompany(Long companyId, Pageable pageable);
    Page<SupportTicketResponse> getByStatus(TicketStatus status, Pageable pageable);
    Page<SupportTicketResponse> getAssignedToMe(Long agentId, Pageable pageable);
    Page<SupportTicketResponse> getMyTickets(Long userId, Pageable pageable);

    void assignToAgent(Long ticketId, Long agentId);
    void reassignToAgent(Long ticketId, Long newAgentId, String reason);
    void escalate(Long ticketId, String reason);

    void recordFirstResponse(Long ticketId);
    void resolve(Long ticketId, String resolutionNotes);
    void close(Long ticketId);
    void reopen(Long ticketId, String reason);

    void recordSatisfaction(Long ticketId, int rating, String feedback);

    List<SupportTicketResponse> getSLABreachedTickets();
    List<SupportTicketResponse> getOpenCriticalTickets();

    SupportTicketResponse update(Long id, SupportTicketRequest request);
    void delete(Long id);

    // CLIENT-facing: raises a CUSTOMER_SUPPORT ticket against the client's own
    // client-company (not the platform), scoped and ownership-checked separately
    // from the staff/platform methods above - see SupportTicketServiceImpl.
    SupportTicketResponse createForClient(SupportTicketRequest request);
    Page<SupportTicketResponse> getMyClientTickets(Pageable pageable);
    SupportTicketResponse getClientTicketById(Long id);

    // STAFF-facing counterpart: this company's own clients' CUSTOMER_SUPPORT
    // tickets (the "Client Chat" inbox), separate from getAll()/getByStatus()
    // above which are PLATFORM_SUPPORT (this company -> BusinessOS) only.
    Page<SupportTicketResponse> getClientTicketsForCompany(Pageable pageable);
    Page<SupportTicketResponse> getClientTicketsForCompanyByStatus(TicketStatus status, Pageable pageable);
}
