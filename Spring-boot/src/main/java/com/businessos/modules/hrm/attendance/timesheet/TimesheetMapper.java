package com.businessos.modules.hrm.attendance.timesheet;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;
import java.time.LocalDateTime;

public class TimesheetMapper {

    /**
     * emp.getUser() forces Hibernate to initialize the Employee proxy, which
     * re-runs the entity's own @SQLRestriction("deleted = false") - a
     * terminated (soft-deleted) employee's proxy throws EntityNotFoundException
     * on any field access beyond its id, which list endpoints hit for every
     * row. Falls back to a label rather than 500ing the whole page.
     */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static TimesheetResponse toTimesheetResponse(Timesheet t) {
        Employee emp = t.getEmployee();
        User empUser = safeUser(emp);
        User approverUser = t.getApprovedBy();

        TimesheetResponse r = new TimesheetResponse();
        r.setId(t.getId());
        r.setWorkDate(t.getWorkDate());
        r.setStartTime(t.getStartTime() != null ? LocalDateTime.of(t.getWorkDate(), t.getStartTime()) : null);
        r.setEndTime(t.getEndTime() != null ? LocalDateTime.of(t.getWorkDate(), t.getEndTime()) : null);
        r.setHoursWorked(t.getHoursWorked());
        r.setBillableHours(t.getBillableHours());
        r.setDescription(t.getWorkSummary());
        r.setProjectName(t.getProjectName());
        r.setTaskDescription(t.getTaskDescription());
        r.setSubmitted(t.isSubmitted());
        r.setSubmittedAt(t.getSubmittedAt());
        r.setApproved(t.isApproved());
        r.setStatus(t.isApproved() ? "APPROVED" : t.isSubmitted() ? "SUBMITTED" : "NOT_SUBMITTED");

        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setApprovedById(approverUser != null ? approverUser.getId() : null);
        r.setApprovedByName(approverUser != null ? approverUser.getFullName() : null);
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}
