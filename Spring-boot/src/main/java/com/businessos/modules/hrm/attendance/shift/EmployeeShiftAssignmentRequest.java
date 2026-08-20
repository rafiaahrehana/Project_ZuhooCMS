package com.businessos.modules.hrm.attendance.shift;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShiftAssignmentRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Shift ID is required")
    private Long shiftId;

    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;
    private String reason;
    private String assignedBy;
    private String notes;
}
