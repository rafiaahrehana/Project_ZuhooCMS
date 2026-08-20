package com.businessos.modules.hrm.attendance.shift;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShiftAssignmentResponse {
    private Long id;
    private Long companyId;
    private Long employeeId;
    private String employeeName;
    private Long shiftId;
    private String shiftName;
    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private boolean active;
    private String reason;
    private String assignedBy;
    private String notes;
}
