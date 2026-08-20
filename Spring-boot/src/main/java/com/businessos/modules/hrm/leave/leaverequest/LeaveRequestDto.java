package com.businessos.modules.hrm.leave.leaverequest;

import jakarta.validation.constraints.NotNull;

import com.businessos.enums.LeaveType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestDto {
    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    private String reason;
}
