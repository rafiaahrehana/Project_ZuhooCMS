package com.businessos.modules.hrm.leave.companyleavePolicy;

import com.businessos.modules.company.CompanyLeavePolicy;

public class CompanyLeavePolicyMapper {

    public static CompanyLeavePolicyResponse toLeavePolicyResponse(CompanyLeavePolicy p) {
        CompanyLeavePolicyResponse r = new CompanyLeavePolicyResponse();
        r.setId(p.getId());
        r.setLeaveType(p.getLeaveType());
        r.setEmploymentType(p.getEmploymentType());
        r.setAnnualEntitlement(p.getAnnualEntitlement());
        r.setMaxCarryForward(p.getMaxCarryForward());
        r.setMaxConsecutiveDays(p.getMaxConsecutiveDays());
        r.setRequiresApproval(p.isRequiresApproval());
        r.setCanCarryForward(p.isCanCarryForward());
        r.setPaid(p.isPaid());
        r.setApplicableFromMonths(p.getApplicableFromMonths());
        r.setActive(p.isActive());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
