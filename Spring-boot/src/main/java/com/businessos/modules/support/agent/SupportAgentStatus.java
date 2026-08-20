package com.businessos.modules.support.agent;

public enum SupportAgentStatus {
    ACTIVE("Active - Available"),
    INACTIVE("Inactive - Not available"),
    ON_BREAK("On Break"),
    OFFLINE("Offline"),
    VACATION("On Vacation");

    private final String description;

    SupportAgentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
