package com.businessos.shared.notification;


public class NotificationPreferenceMapper {


    public static NotificationPreferenceResponse toResponse(NotificationPreference p) {
        NotificationPreferenceResponse r = new NotificationPreferenceResponse();
        r.setId(p.getId());
        r.setEmailOnServiceRequest(p.isEmailOnServiceRequest());
        r.setEmailOnStatusChange(p.isEmailOnStatusChange());
        r.setEmailOnInvoice(p.isEmailOnInvoice());
        r.setEmailOnPayment(p.isEmailOnPayment());
        r.setEmailOnTaskAssigned(p.isEmailOnTaskAssigned());
        r.setEmailOnLeaveUpdate(p.isEmailOnLeaveUpdate());
        r.setInAppOnServiceRequest(p.isInAppOnServiceRequest());
        r.setInAppOnStatusChange(p.isInAppOnStatusChange());
        r.setEmailMarketing(p.isEmailMarketing());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
