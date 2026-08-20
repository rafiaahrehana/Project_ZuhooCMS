package com.businessos.modules.itam.software;

public enum LicenseType {
    PERPETUAL("Perpetual - One time purchase"),
    SUBSCRIPTION("Subscription - Recurring billing"),
    TRIAL("Trial - Limited time"),
    OPEN_SOURCE("Open Source - Free");

    private final String description;

    LicenseType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
