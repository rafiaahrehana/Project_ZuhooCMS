package com.businessos.enums;

/**
 * Lifecycle state of a registered company.
 *
 * PENDING_VERIFICATION — registered, owner email not yet verified
 * TRIAL               — email verified, within 14-day trial window
 * ACTIVE              — paid subscription, fully operational
 * SUSPENDED           — subscription expired, read-only access
 * DEACTIVATED         — permanently disabled by SUPER_ADMIN
 */
public enum CompanyStatus {
    PENDING_VERIFICATION,
    TRIAL,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
