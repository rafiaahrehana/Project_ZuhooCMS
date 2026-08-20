package com.businessos.modules.hrm.attendance.timesheet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimesheetRequest {
    @NotNull(message = "Work date is required")
    private LocalDate workDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @NotNull(message = "Hours worked is required")
    @Min(value = 0)
    private Double hoursWorked;
    private Double billableHours;
    @Size(max = 150)
    private String projectName;
    private String taskDescription;
    private String description;
    private Long taskId;
}
