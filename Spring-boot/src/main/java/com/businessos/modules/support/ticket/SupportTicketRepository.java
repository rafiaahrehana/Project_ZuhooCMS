package com.businessos.modules.support.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Optional<SupportTicket> findByIdAndCompanyId(Long id, Long companyId);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Page<SupportTicket> findByCompanyIdAndStatus(Long companyId, TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Page<SupportTicket> findByCompanyId(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Page<SupportTicket> findByAssignedToAgentId(Long agentId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user"})
    Page<SupportTicket> findByCreatedById(Long userId, Pageable pageable);

    // Previously only matched status = OPEN, so a ticket sitting NEW/IN_PROGRESS/
    // WAITING/ON_HOLD/REOPENED past its deadline was invisible here - now matches
    // anything not already resolved/closed, same "closed statuses" pattern used
    // by the SLA scheduler.
    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN :closedStatuses AND t.slaBreached = false AND t.resolutionDeadline < :now")
    List<SupportTicket> findSLABreachedTickets(@Param("closedStatuses") List<TicketStatus> closedStatuses, @Param("now") LocalDateTime now);

    // Company-scoped variants for the tenant-facing service (loaded every tenant's
    // rows and filtered in application code instead of at the database). The
    // cross-company versions above stay for the scheduler and platform-staff views.
    @Query("SELECT t FROM SupportTicket t WHERE t.companyId = :companyId AND t.status NOT IN :closedStatuses AND t.slaBreached = false AND t.resolutionDeadline < :now")
    List<SupportTicket> findSLABreachedTickets(@Param("companyId") Long companyId,
            @Param("closedStatuses") List<TicketStatus> closedStatuses, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM SupportTicket t WHERE t.resolutionDeadline < :now AND t.slaBreached = false AND t.status NOT IN :closedStatuses")
    List<SupportTicket> findNewlyBreached(@Param("now") LocalDateTime now, @Param("closedStatuses") List<TicketStatus> closedStatuses);

    @Modifying
    @Query("UPDATE SupportTicket t SET t.slaBreached = true WHERE t.resolutionDeadline < :now AND t.slaBreached = false AND t.status NOT IN :closedStatuses")
    int bulkMarkSlaBreaches(@Param("now") LocalDateTime now, @Param("closedStatuses") List<TicketStatus> closedStatuses);

    @Query("SELECT t FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.priority = 'CRITICAL'")
    List<SupportTicket> findOpenCriticalTickets();

    @Query("SELECT t FROM SupportTicket t WHERE t.companyId = :companyId AND t.status IN ('OPEN', 'IN_PROGRESS') AND t.priority = 'CRITICAL'")
    List<SupportTicket> findOpenCriticalTickets(@Param("companyId") Long companyId);

    long countByStatusAndCompanyId(TicketStatus status, Long companyId);

    long countByCompanyIdAndStatusAndCreatedAtBetween(
            Long companyId, TicketStatus status, LocalDateTime from, LocalDateTime to);

    long countByAssignedToAgentId(Long agentId);

    long countByAssignedToAgentIdAndStatusNotIn(Long agentId, List<TicketStatus> closedStatuses);

    Page<SupportTicket> findByCompanyIdAndTitleContainingIgnoreCase(Long companyId, String keyword, Pageable pageable);

    // CLIENT-facing (CUSTOMER_SUPPORT tickets): scoped to both the tenant company AND
    // the specific client, so one client can never see another client's ticket even
    // within the same company - findByIdAndCompanyId alone isn't enough for that.
    @EntityGraph(attributePaths = {"createdBy", "category", "assignedEmployee", "assignedEmployee.user"})
    Page<SupportTicket> findByClientIdAndCompanyId(Long clientId, Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedEmployee", "assignedEmployee.user"})
    Optional<SupportTicket> findByIdAndClientIdAndCompanyId(Long id, Long clientId, Long companyId);

    // Type-scoped variants of findByCompanyId/findByCompanyIdAndStatus - without
    // these, getAll()/getByStatus() mixed CUSTOMER_SUPPORT tickets (a company's
    // own clients messaging them) into what was meant to be strictly this
    // company's PLATFORM_SUPPORT inbox (tickets to BusinessOS), and vice versa.
    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user", "assignedEmployee", "assignedEmployee.user"})
    Page<SupportTicket> findByCompanyIdAndTicketType(Long companyId, TicketType ticketType, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "category", "assignedToAgent", "assignedToAgent.user", "assignedEmployee", "assignedEmployee.user"})
    Page<SupportTicket> findByCompanyIdAndTicketTypeAndStatus(Long companyId, TicketType ticketType, TicketStatus status, Pageable pageable);
}
