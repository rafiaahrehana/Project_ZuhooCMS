package com.businessos.modules.hrm.attendance.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeShiftAssignmentService {
    EmployeeShiftAssignmentResponse create(EmployeeShiftAssignmentRequest request);
    EmployeeShiftAssignmentResponse getById(Long id);
    Page<EmployeeShiftAssignmentResponse> getAll(Pageable pageable);
    EmployeeShiftAssignmentResponse getByEmployee(Long employeeId);
    Page<EmployeeShiftAssignmentResponse> getByShift(Long shiftId, Pageable pageable);
    EmployeeShiftAssignmentResponse update(Long id, EmployeeShiftAssignmentRequest request);
    void endAssignment(Long id);
    EmployeeShiftAssignmentResponse delete(Long id);
}
