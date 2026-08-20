package com.businessos.modules.hrm.leave.leavebalance;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class LeaveBalanceMapper {
    public static LeaveBalanceResponse toLeaveBalanceResponse(LeaveBalance lb) {
        Employee emp = lb.getEmployee();
        User empUser = emp != null ? emp.getUser() : null;

        LeaveBalanceResponse r = new LeaveBalanceResponse();
        r.setId(lb.getId());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setLeaveType(lb.getLeaveType());
        r.setYear(lb.getYear());
        r.setEntitledDays(lb.getTotalDays());
        r.setUsedDays(lb.getUsedDays());
        r.setPendingDays(lb.getPendingDays());
        r.setRemainingDays(lb.getRemainingDays());
        return r;
    }
}
