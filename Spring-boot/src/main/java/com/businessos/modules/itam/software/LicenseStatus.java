package com.businessos.modules.itam.software;

public enum LicenseStatus {
    ACTIVE("Active - Currently licensed"),
    EXPIRED("Expired - License ended"),
    EXPIRING_SOON("Expiring soon - Renewal needed"),
    SUSPENDED("Suspended - Payment issue"),
    REVOKED("Revoked - License cancelled");

    private final String description;

    LicenseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
