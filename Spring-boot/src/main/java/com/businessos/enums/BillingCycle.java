package com.businessos.enums;

import java.time.LocalDate;

public enum BillingCycle {

    /** Charged every month. endDate = startDate + 1 month. */
    MONTHLY,

    /** Charged every 3 months. endDate = startDate + 3 months. */
    QUARTERLY,

    /** Charged annually. endDate = startDate + 1 year. */
    YEARLY,

    /** Single charge, no renewal. endDate = startDate + estimatedDays or explicit. */
    ONE_TIME;

    /** Adds one billing period to the given date. ONE_TIME returns null (never expires automatically). */
    public LocalDate addTo(LocalDate start) {
        return switch (this) {
            case MONTHLY   -> start.plusMonths(1);
            case QUARTERLY -> start.plusMonths(3);
            case YEARLY    -> start.plusYears(1);
            case ONE_TIME  -> null;
        };
    }
}
