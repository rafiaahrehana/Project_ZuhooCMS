package com.businessos.modules.crm.activity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CrmActivityResponse {
    private Long id;
    private CrmActivityType type;
    private String subject;
    private String description;
    private LocalDateTime activityDate;
    private LocalDateTime scheduledAt;
    private boolean completed;
    private boolean systemGenerated;
    private Long clientId;
    private Long opportunityId;
    private String opportunityName;
    private Long performedById;
    private String performedByName;
    private LocalDateTime createdAt;
}
