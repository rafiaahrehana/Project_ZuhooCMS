package com.businessos.enums;

public enum SubscriptionStatus {

    /** Payment confirmed, services accessible. */
    ACTIVE,

    /** Awaiting first payment or renewal payment. */
    PENDING_PAYMENT,

    /** Subscription period ended, not renewed. */
    EXPIRED,

    /** Admin-initiated temporary hold — client cannot raise requests. */
    SUSPENDED,

    /** Client or admin terminated — permanently closed. */
    CANCELLED
}
