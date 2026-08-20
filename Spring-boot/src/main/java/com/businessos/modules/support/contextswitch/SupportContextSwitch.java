package com.businessos.modules.support.contextswitch;

import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_context_switches", indexes = {
        @Index(name = "idx_switch_agent", columnList = "agent_id"),
        @Index(name = "idx_switch_company", columnList = "company_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportContextSwitch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User supportAgent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company viewedCompany; // Company data being viewed

    private LocalDateTime switchedInTime;
    private LocalDateTime switchedOutTime;

    private String purpose; // Why viewing - e.g., "Troubleshooting ticket TKT-001"

    private String ipAddress;
    private String userAgent;

    @Builder.Default
    private boolean stillActive = true;
}