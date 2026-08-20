package com.businessos.modules.support.sla;

import com.businessos.modules.support.ticket.TicketPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// AllArgsConstructor access is package-private: a public one is picked up by Jackson as
// a deserialization creator, which fails on any missing primitive field ("Cannot map
// null into type int/boolean") instead of defaulting it to 0/false via no-args+setters.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SLAPolicyRequest {

    @NotNull(message = "Policy name is required")
    private String policyName;

    private String description;

    @NotNull(message = "Applicable priority is required")
    private TicketPriority applicablePriority;

    private int firstResponseTimeHours;
    private int resolutionTimeHours;
    private boolean businessHoursOnly;
    private boolean active;
    private String notes;
}
