package com.businessos.modules.hrm.attendance.timesheet;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TimesheetComposeRequest {
    private String projectName;
    @NotBlank
    private String roughNotes;
}
