package com.businessos.modules.hrm.attendance.shift;

public class EmployeeShiftAssignmentMapper {

    /** A terminated employee's lazy proxy throws on any field access beyond its id - see OffboardingChecklistMapper.safeFullName. */
    private static String safeFullName(com.businessos.modules.hrm.employee.Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getFullName();
        } catch (Exception e) {
            return null;
        }
    }

    public static EmployeeShiftAssignmentResponse toResponse(EmployeeShiftAssignment entity) {
        if (entity == null) {
            return null;
        }

        return EmployeeShiftAssignmentResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(safeFullName(entity.getEmployee()))
                .shiftId(entity.getShift() != null ? entity.getShift().getId() : null)
                .shiftName(entity.getShift() != null ? entity.getShift().getName() : null)
                .assignmentStartDate(entity.getAssignmentStartDate())
                .assignmentEndDate(entity.getAssignmentEndDate())
                .active(entity.isActive())
                .reason(entity.getReason())
                .assignedBy(entity.getAssignedBy())
                .notes(entity.getNotes())
                .build();
    }
}
