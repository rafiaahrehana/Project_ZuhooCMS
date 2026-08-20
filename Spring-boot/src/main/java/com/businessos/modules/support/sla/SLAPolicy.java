package com.businessos.modules.support.sla;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.support.ticket.TicketPriority;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SLAPolicy extends BaseEntity {

    private String policyName; // e.g., "Standard SLA"

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TicketPriority applicablePriority;

    // Response time in hours
    private int firstResponseTimeHours;

    // Resolution time in hours
    private int resolutionTimeHours;

    // Business hours only?
    @Builder.Default
    private boolean businessHoursOnly = true;

    @Builder.Default
    private boolean active = true;

    private String notes;
}