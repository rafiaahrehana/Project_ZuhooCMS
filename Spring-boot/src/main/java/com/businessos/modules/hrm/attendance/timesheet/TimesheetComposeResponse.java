package com.businessos.modules.hrm.attendance.timesheet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetComposeResponse {
    private String taskDescription;
    private String description;
}
