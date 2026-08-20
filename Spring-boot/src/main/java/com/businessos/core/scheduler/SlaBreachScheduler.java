package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.enums.ServiceRequestStatus;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.modules.support.ticket.SupportTicket;
import com.businessos.modules.support.ticket.SupportTicketRepository;
import com.businessos.modules.support.ticket.TicketStatus;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor

public class SlaBreachScheduler {

    private final ServiceRequestRepository serviceRequestRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;

    private static final List<ServiceRequestStatus> CLOSED_STATUSES = List.of(
            ServiceRequestStatus.COMPLETED,
            ServiceRequestStatus.CANCELLED,
            ServiceRequestStatus.REJECTED
    );

    // Only RESOLVED/CLOSED are terminal - REOPENED, and everything before
    // RESOLVED, can still be past its deadline.
    private static final List<TicketStatus> CLOSED_TICKET_STATUSES = List.of(
            TicketStatus.RESOLVED,
            TicketStatus.CLOSED
    );

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void markSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();

        List<ServiceRequest> newlyBreached =
                serviceRequestRepository.findNewlyBreached(now, CLOSED_STATUSES);

        serviceRequestRepository.bulkMarkSlaBreaches(now, CLOSED_STATUSES);

        for (ServiceRequest request : newlyBreached) {
            if (request.getAssignedEmployee() == null
                    || request.getAssignedEmployee().getUser() == null) {
                continue;
            }
            notificationService.sendForServiceRequest(CreateNotificationRequest.forRequest(
                    NotificationType.SLA_BREACHED,
                    "SLA breached",
                    "Service request \"" + request.getTitle() + "\" has passed its SLA deadline",
                    request.getAssignedEmployee().getUser().getId(),
                    request.getCompany().getId(),
                    request.getId()
            ));
        }

        markTicketSlaBreaches(now);
    }

    // Previously the support ticket desk had no SLA-breach automation at all -
    // SupportTicket.slaBreached was initialized false and never set anywhere,
    // and the one dashboard that read it only matched status = OPEN. A ticket
    // sitting NEW/IN_PROGRESS/WAITING/ON_HOLD past its deadline paged no one.
    private void markTicketSlaBreaches(LocalDateTime now) {
        List<SupportTicket> newlyBreached =
                supportTicketRepository.findNewlyBreached(now, CLOSED_TICKET_STATUSES);

        supportTicketRepository.bulkMarkSlaBreaches(now, CLOSED_TICKET_STATUSES);

        for (SupportTicket ticket : newlyBreached) {
            if (ticket.getAssignedToAgent() == null || ticket.getAssignedToAgent().getUser() == null) {
                continue;
            }
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.SLA_BREACHED,
                    "SLA breached",
                    "Ticket \"" + ticket.getTitle() + "\" (" + ticket.getTicketNumber() + ") has passed its SLA deadline",
                    "/support/tickets/" + ticket.getId(),
                    ticket.getAssignedToAgent().getUser().getId(),
                    ticket.getCompanyId()
            ));
        }
    }
}
