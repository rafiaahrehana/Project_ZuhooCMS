package com.businessos.modules.support.sla;

import com.businessos.modules.support.ticket.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLAPolicyResponse {
    private Long id;
    private String policyName;
    private String description;
    private TicketPriority applicablePriority;
    private int firstResponseTimeHours;
    private int resolutionTimeHours;
    private boolean businessHoursOnly;
    private boolean active;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
