package com.businessos.shared.notification;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationPreferenceResponse {
    private Long id;
    private boolean emailOnServiceRequest;
    private boolean emailOnStatusChange;
    private boolean emailOnInvoice;
    private boolean emailOnPayment;
    private boolean emailOnTaskAssigned;
    private boolean emailOnLeaveUpdate;
    private boolean inAppOnServiceRequest;
    private boolean inAppOnStatusChange;
    private boolean emailMarketing;
    private LocalDateTime updatedAt;
}
