package com.businessos.shared.notification;

import com.businessos.enums.NotificationType;
import lombok.Getter;

/**
 * Internal DTO used by services to dispatch notifications.
 * Not exposed as an HTTP endpoint.
 */
@Getter
public class CreateNotificationRequest {
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String actionUrl;
    private final Long recipientId;
    private final Long companyId;
    private final Long serviceRequestId;

    private CreateNotificationRequest(NotificationType type, String title, String message,
                                       String actionUrl, Long recipientId, Long companyId,
                                       Long serviceRequestId) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.recipientId = recipientId;
        this.companyId = companyId;
        this.serviceRequestId = serviceRequestId;
    }

    public static CreateNotificationRequest of(NotificationType type, String title, String message,
                                                String actionUrl, Long recipientId, Long companyId) {
        return new CreateNotificationRequest(type, title, message, actionUrl, recipientId, companyId, null);
    }

    public static CreateNotificationRequest forRequest(NotificationType type, String title, String message,
                                                        Long recipientId, Long companyId, Long serviceRequestId) {
        return new CreateNotificationRequest(type, title, message,
                "/servicereview-requests/" + serviceRequestId, recipientId, companyId, serviceRequestId);
    }
}
