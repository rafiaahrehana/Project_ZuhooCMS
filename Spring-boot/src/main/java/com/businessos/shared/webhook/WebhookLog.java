package com.businessos.shared.webhook;

import com.businessos.enums.WebhookStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_logs", indexes = {
    @Index(name = "idx_webhook_retry", columnList = "status, nextRetryAt"),
    @Index(name = "idx_webhook_transaction", columnList = "transactionId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider; // BKASH, NAGAD, SSLCOMMERZ

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String transactionId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status = WebhookStatus.RECEIVED;

    @Builder.Default
    private int retryCount = 0;
    private String failureReason;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    private LocalDateTime processedAt;
    private LocalDateTime nextRetryAt;
}
