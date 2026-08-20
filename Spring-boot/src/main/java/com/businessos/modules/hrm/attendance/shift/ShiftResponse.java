package com.businessos.modules.hrm.attendance.shift;
import com.businessos.modules.hrm.attendance.attendance.ShiftType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class ShiftResponse {
    private Long id;
    private String name;
    private ShiftType shiftType;
    private LocalTime startTime;
    private LocalTime endTime;
    private int gracePeriodMinutes;
    private String weeklyOffDays;
    private boolean flexible;
    private boolean nightShift;
    private boolean active;
    private long workingMinutes;
    private String description;
    private String notes;
    private LocalDateTime createdAt;
}
