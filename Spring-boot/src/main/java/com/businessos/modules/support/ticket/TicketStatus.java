package com.businessos.modules.support.ticket;

public enum TicketStatus {
    NEW("New - Just created"),
    OPEN("Open - Being reviewed"),
    IN_PROGRESS("In Progress - Agent working"),
    WAITING("Waiting - Awaiting customer response"),
    ON_HOLD("On Hold - Temporarily stopped"),
    RESOLVED("Resolved - Issue fixed"),
    CLOSED("Closed - Ticket completed"),
    REOPENED("Reopened - Customer reopened");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
