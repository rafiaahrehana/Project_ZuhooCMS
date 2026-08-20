package com.businessos.modules.support.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportAgentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String fullName;
    private String email;
    private String department;
    private String specialization;
    private SupportAgentStatus status;
    private int totalTicketsHandled;
    private double avgResponseTimeMinutes;
    private double avgResolutionTimeMinutes;
    private double satisfactionScore;
    private LocalDateTime lastActiveTime;
    private boolean acceptingTickets;
    private int maxConcurrentTickets;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
