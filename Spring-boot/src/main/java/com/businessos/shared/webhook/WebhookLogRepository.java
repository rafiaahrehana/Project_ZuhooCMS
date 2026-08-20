package com.businessos.shared.webhook;

import com.businessos.enums.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    Optional<WebhookLog> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    // Retry scheduler query — pick up failed webhooks ready for retry
    List<WebhookLog> findByStatusAndRetryCountLessThanAndNextRetryAtBefore(
        WebhookStatus status, int maxRetries, LocalDateTime now);
}
