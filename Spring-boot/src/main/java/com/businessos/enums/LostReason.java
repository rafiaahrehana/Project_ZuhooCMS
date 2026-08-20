package com.businessos.enums;

/**
 * Why an opportunity was lost, as a picklist.
 *
 * The free-text lostReason field predates this and stays as the optional
 * detail; the code is what makes win/loss analysis aggregatable - "we lost 40%
 * on price" is unanswerable over free text.
 */
public enum LostReason {
    PRICE,
    COMPETITOR,
    NO_BUDGET,
    NO_RESPONSE,
    BAD_TIMING,
    REQUIREMENTS_MISMATCH,
    OTHER
}
