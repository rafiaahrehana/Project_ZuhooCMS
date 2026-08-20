package com.raf.zuhoo.data.model;

// Mirrors backend com.zuhoo.enums.SubscriptionStatus.
public final class SubscriptionStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String EXPIRED = "EXPIRED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String CANCELLED = "CANCELLED";

    private SubscriptionStatus() {
    }
}
