package com.businessos.shared.notification;

import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.auth.user.User;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.device.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Notification dispatch service.
 *
 * Persistence-first model:
 *   1. Persist the Notification to the DB — guarantees delivery even
 *      if the WebSocket connection is not active.
 *   2. Push via WebSocket to /user/{userId}/queue/notifications.
 *   3. On WebSocket reconnect, Angular polls GET /api/notifications?unreadOnly=true
 *      to catch any notifications missed during disconnection.
 *
 * All send() calls are @Async — they never block the calling thread.
 *
 * Deduplication: sendForServiceRequest() checks for an existing notification
 * of the same type + requestId + recipient before persisting, preventing
 * repeated SLA breach or assignment notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository  notificationRepository;
    private final UserRepository          userRepository;
    private final SimpMessagingTemplate   messagingTemplate;
    private final SecurityUtil            securityUtil;
    private final FcmPushService          fcmPushService;

    @Override
    @Async
    @Transactional
    public void send(CreateNotificationRequest request) {
        User recipient = userRepository.findById(request.getRecipientId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Recipient not found: " + request.getRecipientId()));

        Notification notification = buildNotification(request, recipient);
        notificationRepository.save(notification);
        pushWebSocket(notification, recipient.getId());
        pushMobile(notification, recipient.getId());
    }

    @Override
    @Async
    @Transactional
    public void sendForServiceRequest(CreateNotificationRequest request) {
        if (request.getServiceRequestId() != null
                && notificationRepository.existsByRecipientIdAndServiceRequestIdAndType(
                    request.getRecipientId(), request.getServiceRequestId(), request.getType())) {
            return;
        }
        User recipient = userRepository.findById(request.getRecipientId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Recipient not found: " + request.getRecipientId()));

        Notification notification = buildNotification(request, recipient);
        notificationRepository.save(notification);
        pushWebSocket(notification, recipient.getId());
        pushMobile(notification, recipient.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(boolean unreadOnly, Pageable pageable) {
        Long userId = securityUtil.getCurrentUser().getId();
        Page<Notification> page = unreadOnly
            ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
            : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount() {
        Long userId = securityUtil.getCurrentUser().getId();
        return new NotificationCountResponse(
            notificationRepository.countByRecipientIdAndReadFalse(userId));
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = securityUtil.getCurrentUser().getId();
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!n.getRecipient().getId().equals(userId)) {
            throw new BadRequestException("You can only mark your own notifications as read");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        Long userId = securityUtil.getCurrentUser().getId();
        notificationRepository.markAllReadForUser(userId, LocalDateTime.now());
    }

    // ── Private helpers ───────────────────────────────────────────

    private Notification buildNotification(CreateNotificationRequest request, User recipient) {
        ServiceRequest sr = null;
        if (request.getServiceRequestId() != null) {
            sr = new ServiceRequest();
            sr.setId(request.getServiceRequestId());
        }
        return Notification.builder()
            .type(request.getType())
            .title(request.getTitle())
            .message(request.getMessage())
            .actionUrl(request.getActionUrl())
            .recipient(recipient)
            .companyId(request.getCompanyId())
            .serviceRequest(sr)
            .build();
    }

    private void pushWebSocket(Notification n, Long userId) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                NotificationMapper.toResponse(n)
            );
        } catch (Exception e) {
            // WebSocket push failure is non-critical — client will poll on reconnect
            log.debug("WebSocket push failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * The WebSocket push above only reaches a client that is currently connected, which on a
     * phone means the app is open. FCM is what reaches it when the app is backgrounded or
     * killed. Same best-effort treatment: the row is already persisted, so a failure here costs
     * a notification's immediacy, never its delivery.
     */
    private void pushMobile(Notification n, Long userId) {
        try {
            fcmPushService.push(n, userId);
        } catch (Exception e) {
            log.debug("Push notification failed for user {}: {}", userId, e.getMessage());
        }
    }
}
