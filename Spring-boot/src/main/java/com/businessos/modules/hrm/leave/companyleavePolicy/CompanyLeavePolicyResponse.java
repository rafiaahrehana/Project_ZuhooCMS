package com.businessos.modules.hrm.leave.companyleavePolicy;

import com.businessos.enums.EmploymentType;
import com.businessos.enums.LeaveType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CompanyLeavePolicyResponse {
    private Long id;
    private LeaveType leaveType;
    private EmploymentType employmentType;
    private int annualEntitlement;
    private int maxCarryForward;
    private Integer maxConsecutiveDays;
    private boolean requiresApproval;
    private boolean canCarryForward;
    private boolean paid;
    private int applicableFromMonths;
    private boolean active;
    private LocalDateTime createdAt;
}
