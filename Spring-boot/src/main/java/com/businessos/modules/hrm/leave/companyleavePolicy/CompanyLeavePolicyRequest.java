package com.businessos.modules.hrm.leave.companyleavePolicy;

import com.businessos.enums.EmploymentType;
import com.businessos.enums.LeaveType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompanyLeavePolicyRequest {
    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;
    private EmploymentType employmentType;
    @NotNull
    @Min(0)
    private Integer annualEntitlement;
    private Integer maxCarryForward;
    private Integer maxConsecutiveDays;
    private boolean requiresApproval;
    private boolean canCarryForward;
    private boolean paid;
    private Integer applicableFromMonths;
}
