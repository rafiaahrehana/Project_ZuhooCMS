package com.businessos.modules.hrm.leave.leaverequest;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class LeaveRequestMapper {

    /** See TimesheetMapper.safeUser - a terminated employee's lazy proxy throws on any field access beyond its id. */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static LeaveRequestResponse toLeaveRequestResponse(LeaveRequest lr) {
        Employee emp = lr.getEmployee();
        User empUser = safeUser(emp);
        User reviewerUser = lr.getReviewedBy();

        LeaveRequestResponse r = new LeaveRequestResponse();
        r.setId(lr.getId());
        r.setLeaveType(lr.getLeaveType());
        r.setStartDate(lr.getStartDate());
        r.setEndDate(lr.getEndDate());
        r.setTotalDays(lr.getTotalDays());
        r.setReason(lr.getReason());
        r.setStatus(lr.getStatus());
        r.setRejectionReason(lr.getRejectionReason());
        r.setReviewedAt(lr.getReviewedAt());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setReviewedById(reviewerUser != null ? reviewerUser.getId() : null);
        r.setReviewedByName(reviewerUser != null ? reviewerUser.getFullName() : null);
        r.setCreatedAt(lr.getCreatedAt());
        return r;
    }
}
