package com.raf.zuhoo.data.model;

// Mirrors backend com.zuhoo.enums.ServiceRequestPriority.
//
// Note NORMAL, not MEDIUM — TicketPriority (a different enum, for support tickets) uses MEDIUM.
// Sending the wrong one is a 400 that reads like a mystery, so keep the two apart.
public final class ServiceRequestPriority {

    public static final String LOW = "LOW";
    public static final String NORMAL = "NORMAL";
    public static final String HIGH = "HIGH";
    public static final String URGENT = "URGENT";

    private ServiceRequestPriority() {
    }
}
