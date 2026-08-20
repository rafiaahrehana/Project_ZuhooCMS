package com.businessos.shared.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /** INTERNAL: persist notification and push via WebSocket — fire and forget */
    void send(CreateNotificationRequest request);

    /** INTERNAL: send notification with duplicate suppression for servicereview requeststatus events */
    void sendForServiceRequest(CreateNotificationRequest request);

    /** ALL: get notification inbox — optionally filter to unread only */
    Page<NotificationResponse> getMyNotifications(boolean unreadOnly, Pageable pageable);

    /** ALL: get unread notification count — for badge display */
    NotificationCountResponse getUnreadCount();

    /** ALL: mark a single notification as read */
    void markAsRead(Long notificationId);

    /** ALL: mark all notifications as read */
    void markAllAsRead();}
