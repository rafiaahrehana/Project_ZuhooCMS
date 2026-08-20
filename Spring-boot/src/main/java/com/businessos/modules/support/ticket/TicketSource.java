package com.businessos.modules.support.ticket;

public enum TicketSource {
    EMAIL("Email"),
    PHONE("Phone"),
    PORTAL("Support Portal"),
    CHAT("Live Chat"),
    SOCIAL_MEDIA("Social Media"),
    IN_APP("In-App Help"),
    OTHER("Other");

    private final String label;

    TicketSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
