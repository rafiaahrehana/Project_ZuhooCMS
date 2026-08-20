package com.businessos.modules.ai.entity;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs", indexes = {
        @Index(name = "idx_ai_usage_company", columnList = "company_id, log_date"),
        @Index(name = "idx_ai_usage_user", columnList = "user_id, log_date"),
        // Backs the rolling hourly per-user quota check (countByUserSince).
        @Index(name = "idx_ai_usage_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_usage_feature", columnList = "company_id, ai_feature"),
        @Index(name = "idx_ai_usage_date", columnList = "log_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiFeature aiFeature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AiProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AiModel model;

    @Column(name = "input_tokens", nullable = false)
    @Builder.Default
    private int inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    @Builder.Default
    private int outputTokens = 0;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
}
