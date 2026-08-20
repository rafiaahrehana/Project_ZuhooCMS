package com.businessos.modules.hrm.attendance.shift;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmployeeShiftAssignmentServiceImpl implements EmployeeShiftAssignmentService {

    private final EmployeeShiftAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public EmployeeShiftAssignmentResponse create(EmployeeShiftAssignmentRequest request) {
        authorizationService.checkPermission(PermissionCode.SHIFT_ASSIGNMENT_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();
        
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Shift shift = shiftRepository.findByIdAndCompanyId(request.getShiftId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        // End any existing active assignments
        assignmentRepository.findByCompanyIdAndEmployeeIdAndActive(companyId, request.getEmployeeId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    assignmentRepository.save(existing);
                });

        EmployeeShiftAssignment assignment = EmployeeShiftAssignment.builder()
                .companyId(companyId)
                .employee(employee)
                .shift(shift)
                .assignmentStartDate(request.getAssignmentStartDate() != null ?
                        request.getAssignmentStartDate() : LocalDate.now())
                .assignmentEndDate(request.getAssignmentEndDate())
                .active(true)
                .reason(request.getReason())
                .assignedBy(request.getAssignedBy())
                .notes(request.getNotes())
                .build();

        assignment = assignmentRepository.save(assignment);
        return EmployeeShiftAssignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeShiftAssignmentResponse getById(Long id) {
        Long companyId = securityUtil.getCurrentCompanyId();
        EmployeeShiftAssignment assignment = assignmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        return EmployeeShiftAssignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeShiftAssignmentResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SHIFT_ASSIGNMENT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return assignmentRepository.findByCompanyId(companyId, pageable)
                .map(EmployeeShiftAssignmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeShiftAssignmentResponse getByEmployee(Long employeeId) {
        Long companyId = securityUtil.getCurrentCompanyId();
        EmployeeShiftAssignment assignment = assignmentRepository
                .findByCompanyIdAndEmployeeIdAndActive(companyId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Active assignment not found"));
        return EmployeeShiftAssignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeShiftAssignmentResponse> getByShift(Long shiftId, Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        return assignmentRepository.findByCompanyIdAndShiftIdAndActiveTrue(companyId, shiftId, pageable)
                .map(EmployeeShiftAssignmentMapper::toResponse);
    }

    @Override
    @Transactional
    public EmployeeShiftAssignmentResponse update(Long id, EmployeeShiftAssignmentRequest request) {
        authorizationService.checkPermission(PermissionCode.SHIFT_ASSIGNMENT_UPDATE);
        Long companyId = securityUtil.getCurrentCompanyId();
        EmployeeShiftAssignment assignment = assignmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        Shift shift = shiftRepository.findByIdAndCompanyId(request.getShiftId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        assignment.setShift(shift);
        assignment.setAssignmentEndDate(request.getAssignmentEndDate());
        assignment.setReason(request.getReason());
        assignment.setNotes(request.getNotes());

        assignment = assignmentRepository.save(assignment);
        return EmployeeShiftAssignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional
    public void endAssignment(Long id) {
        authorizationService.checkPermission(PermissionCode.SHIFT_ASSIGNMENT_UPDATE);
        Long companyId = securityUtil.getCurrentCompanyId();
        EmployeeShiftAssignment assignment = assignmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        assignment.setActive(false);
        assignment.setAssignmentEndDate(LocalDate.now());
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public EmployeeShiftAssignmentResponse delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SHIFT_ASSIGNMENT_DELETE);
        Long companyId = securityUtil.getCurrentCompanyId();
        EmployeeShiftAssignment assignment = assignmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        assignment.softDelete();
        assignmentRepository.save(assignment);
        return EmployeeShiftAssignmentMapper.toResponse(assignment);
    }
}
