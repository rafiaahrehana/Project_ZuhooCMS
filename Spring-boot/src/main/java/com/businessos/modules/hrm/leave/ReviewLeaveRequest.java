package com.businessos.modules.hrm.leave;

import com.businessos.enums.LeaveRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewLeaveRequest {
    @NotNull(message = "Status is required")
    private LeaveRequestStatus status;
    private String rejectionReason;
}
