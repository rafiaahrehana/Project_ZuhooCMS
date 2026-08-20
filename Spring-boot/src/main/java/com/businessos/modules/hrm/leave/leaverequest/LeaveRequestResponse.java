package com.businessos.modules.hrm.leave.leaverequest;

import com.businessos.enums.LeaveRequestStatus;
import com.businessos.enums.LeaveType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class LeaveRequestResponse {
    private Long id;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private String reason;
    private LeaveRequestStatus status;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private Long employeeId;
    private String employeeName;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime createdAt;
}
