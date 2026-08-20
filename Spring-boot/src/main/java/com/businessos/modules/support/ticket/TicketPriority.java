package com.businessos.modules.support.ticket;

public enum TicketPriority {
    CRITICAL("Critical - System down", 1),
    HIGH("High - Major feature broken", 2),
    MEDIUM("Medium - Normal issue", 3),
    LOW("Low - Minor issue", 4);

    private final String description;
    private final int level;

    TicketPriority(String description, int level) {
        this.description = description;
        this.level = level;
    }

    public String getDescription() { return description; }
    public int getLevel() { return level; }
}