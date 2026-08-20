package com.businessos.shared.notification;

import com.businessos.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
        Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.recipient.id = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff AND n.read = true")
    int deleteReadOlderThan(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByRecipientIdAndServiceRequestIdAndType(
        Long recipientId, Long serviceRequestId, NotificationType type);
}
