package com.businessos.shared.notification;

import lombok.Data;

@Data
public class UpdateNotificationPreferenceRequest {
    private boolean emailOnServiceRequest;
    private boolean emailOnStatusChange;
    private boolean emailOnInvoice;
    private boolean emailOnPayment;
    private boolean emailOnTaskAssigned;
    private boolean emailOnLeaveUpdate;
    private boolean inAppOnServiceRequest;
    private boolean inAppOnStatusChange;
    private boolean emailMarketing;
}
