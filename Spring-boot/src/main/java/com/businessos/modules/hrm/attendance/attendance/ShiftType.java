package com.businessos.modules.hrm.attendance.attendance;

public enum ShiftType {
    MORNING("Morning - 9 AM to 1 PM"),
    AFTERNOON("Afternoon - 1 PM to 5 PM"),
    FULL_DAY("Full Day - 9 AM to 5 PM"),
    EVENING("Evening - 5 PM to 9 PM"),
    NIGHT("Night - 9 PM to 6 AM"),
    FLEXIBLE("Flexible - No fixed hours");

    private final String description;

    ShiftType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
