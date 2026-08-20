package com.businessos.modules.support.ticket;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketResponse {

    private Long id;
    private Long companyId;
    private String ticketNumber;
    private TicketType ticketType;

    private String title;
    private String description;
    private String attachmentUrl;
    private String attachmentFileName;

    private String categoryName;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketSource source;

    private Long assignedToAgentId;
    private String assignedToAgentName;
    // CUSTOMER_SUPPORT tickets route to an internal Employee instead of a
    // platform SupportAgent - see SupportTicket.assignedEmployee.
    private String assignedEmployeeName;
    private LocalDateTime assignedDate;

    private LocalDateTime firstResponseTime;
    private LocalDateTime resolutionTime;

    private LocalDateTime firstResponseDeadline;
    private LocalDateTime resolutionDeadline;
    private boolean slaBreached;

    private String resolutionNotes;
    private LocalDateTime closedDate;

    private Integer satisfactionRating;
    private String satisfactionFeedback;

    private int escalationLevel;
    private LocalDateTime escalatedDate;

    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
