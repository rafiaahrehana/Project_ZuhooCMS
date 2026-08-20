package com.businessos.modules.support.agent;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_agents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportAgent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String department; // Support Level 1, Level 2, etc.

    private String specialization; // Payment, Technical, Billing, etc.

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private SupportAgentStatus status = SupportAgentStatus.ACTIVE;

    @Builder.Default
    private int totalTicketsHandled = 0;

    @Builder.Default
    private double avgResponseTimeMinutes = 0.0;

    @Builder.Default
    private double avgResolutionTimeMinutes = 0.0;

    @Builder.Default
    private double satisfactionScore = 0.0; // 0-5 stars average

    private LocalDateTime lastActiveTime;

    @Builder.Default
    private boolean acceptingTickets = true;

    @Builder.Default
    private int maxConcurrentTickets = 10;

    private String notes;
}
