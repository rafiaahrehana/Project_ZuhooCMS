package com.businessos.enums;

/**
 * How a single day's pay is derived from a monthly salary, for absence
 * deductions and hourly rates.
 *
 * There is no universally correct answer - it is a company policy, and it
 * differs by industry and country, so each tenant picks its own.
 */
public enum PerDayBasis {

    /**
     * Divide by the number of days in that calendar month (28-31).
     * Simple and common across South Asia. Note that the same absence costs
     * more in February than in January.
     */
    CALENDAR_DAYS,

    /** Fixed 30-day month. Predictable and identical every month. */
    FIXED_30,

    /**
     * Fixed 26-day month - 30 days less roughly four weekly holidays. A
     * manufacturing and RMG convention; the smaller divisor makes each day
     * worth more, so deductions bite harder.
     */
    FIXED_26,

    /**
     * The actual working days in the month, excluding weekly-off days and
     * company holidays. The most defensible basis, but the divisor moves
     * between 20 and 23 from month to month.
     */
    ACTUAL_WORKING_DAYS
}
