package com.businessos.auth.role.enums;

public enum Role {

    // PLATFORM ROLES (SaaS Provider Staff)

    // Ultimate owner of the SaaS platform. Has full access to everything.
    SUPER_ADMIN,

    // Manages platform configuration and global users.
    SYSTEM_ADMIN,

    // Resolves tickets and supports tenant companies. Has no access to tenant
    // private data.
    SUPPORT_AGENT,

    // Manages the support team and oversees escalated tickets.
    SUPPORT_MANAGER,

    // Manages platform promotions, campaigns, and overall SaaS marketing.
    MARKETING_MANAGER,

    // Manages SaaS billing, subscriptions, invoices, and revenue tracking.
    PLATFORM_ACCOUNTANT,

    // Onboards new enterprise clients and manages custom pricing/packages.
    SALES_MANAGER,

    // COMPANY ROLES (Tenant Staff)

    // The creator/owner of a tenant company. Has full access within their company.
    COMPANY_OWNER,

    // External client/customer of a tenant company.
    CLIENT,

    // Standard worker/staff of a tenant company. Subject to CustomRole permissions.
    EMPLOYEE
}
