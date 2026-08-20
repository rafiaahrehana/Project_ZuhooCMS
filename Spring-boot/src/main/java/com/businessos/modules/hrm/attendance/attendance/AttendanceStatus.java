package com.businessos.modules.hrm.attendance.attendance;

public enum AttendanceStatus {
    PRESENT("Present - Marked on time"),
    LATE("Late - Marked after work start"),
    ABSENT("Absent - Did not mark"),
    ON_LEAVE("On Leave - Approved leave"),
    HALF_DAY("Half Day - Partial attendance"),
    WORK_FROM_HOME("Work From Home - Remote"),
    WEEKEND("Weekend - Non-working day"),
    HOLIDAY("Holiday - Public holiday"),
    PARTIAL_DAY("Partial Day - Left early"),
    UNMARKED("Unmarked - No attendance record");

    private final String description;

    AttendanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
