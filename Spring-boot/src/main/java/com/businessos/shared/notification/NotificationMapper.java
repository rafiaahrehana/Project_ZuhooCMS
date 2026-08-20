package com.businessos.shared.notification;

import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;

public class NotificationMapper {


    public static NotificationResponse toResponse(Notification n) {
        ServiceRequest sr = n.getServiceRequest();
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setActionUrl(n.getActionUrl());
        r.setRead(n.isRead());
        r.setReadAt(n.getReadAt());
        r.setServiceRequestId(sr != null ? sr.getId() : null);
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
