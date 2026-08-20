package com.businessos.modules.itam.software;

public enum LicenseRenewalType {
    ANNUAL("Annual renewal"),
    MONTHLY("Monthly billing"),
    BIENNIAL("Every 2 years"),
    PERPETUAL("No renewal needed");

    private final String label;

    LicenseRenewalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
