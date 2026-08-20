package com.businessos.modules.hrm.attendance.shift;


import com.businessos.modules.hrm.attendance.attendance.ShiftType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ShiftRequest {
    @NotBlank(message = "Shift name is required")
    @Size(max = 100)
    private String name;
    @NotNull(message = "Shift type is required")
    private ShiftType shiftType;
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    private Integer gracePeriodMinutes;
    private String weeklyOffDays;
    private boolean flexible;
    private boolean nightShift;
    private String description;
    private String notes;
}
