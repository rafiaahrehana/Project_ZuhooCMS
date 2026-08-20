package com.businessos.shared.notification;

import com.businessos.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private boolean read;
    private LocalDateTime readAt;
    private Long serviceRequestId;
    private LocalDateTime createdAt;
}
