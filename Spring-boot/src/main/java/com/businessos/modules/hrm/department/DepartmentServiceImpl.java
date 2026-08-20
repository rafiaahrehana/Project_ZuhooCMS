package com.businessos.modules.hrm.department;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.company.Company;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil         securityUtil;
    private final AuthorizationService authorizationService;
    private final com.businessos.shared.notification.NotificationService notificationService;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        authorizationService.checkPermission(PermissionCode.DEPARTMENT_CREATE);
        Long companyId = requireCompanyId();

        if (departmentRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException("A department named '" + request.getName() + "' already exists");
        }

        Department dept = Department.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .budget(request.getBudget())
            .company(companyRef(companyId))
            .build();

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findByIdAndCompanyId(
                    request.getParentDepartmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Parent department not found: " + request.getParentDepartmentId()));
            dept.setDepartment(parent);
        }
        if (request.getHeadEmployeeId() != null) {
            dept.setEmployee(findEmployeeInTenant(request.getHeadEmployeeId(), companyId));
        }

        departmentRepository.save(dept);
        
        return DepartmentMapper.toResponse(dept);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        return DepartmentMapper.toResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> listAll(Pageable pageable) {
        // Deliberately NOT gated by DEPARTMENT_VIEW: this endpoint doubles as a
        // cross-module picker (Announcements, Holidays, Job Postings all use it to
        // populate a department dropdown) for users who may lack DEPARTMENT_VIEW but
        // hold whatever permission actually governs that other module. The HRM
        // "Departments" admin page is gated at the frontend sidebar/route level only,
        // until this endpoint is split into a full admin view vs a lightweight picker.
        return departmentRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(DepartmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listActive() {
        return departmentRepository.findByCompanyIdAndActiveTrue(requireCompanyId())
            .stream().map(DepartmentMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        authorizationService.checkPermission(PermissionCode.DEPARTMENT_UPDATE);
        Long companyId = requireCompanyId();
        Department dept = findInTenant(id);

        if (!dept.getName().equals(request.getName())
                && departmentRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException("A department named '" + request.getName() + "' already exists");
        }

        dept.setName(request.getName());
        if (request.getCode()        != null) dept.setCode(request.getCode());
        if (request.getDescription() != null) dept.setDescription(request.getDescription());
        if (request.getBudget()      != null) dept.setBudget(request.getBudget());

        if (request.getParentDepartmentId() != null) {
            if (request.getParentDepartmentId().equals(id)) {
                throw new BadRequestException("A department cannot be its own parent");
            }
            Department parent = departmentRepository.findByIdAndCompanyId(
                    request.getParentDepartmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Parent department not found: " + request.getParentDepartmentId()));

            // The direct-reference check above only catches A -> A. Walking the new
            // parent's own chain catches the longer cycle too (A -> B -> A), which
            // would otherwise corrupt the hierarchy - anything reading up the parent
            // chain would loop forever.
            Department walker = parent;
            while (walker != null) {
                if (walker.getId().equals(id)) {
                    throw new BadRequestException(
                        "Cannot set this parent - it would create a department hierarchy cycle");
                }
                walker = walker.getDepartment();
            }

            dept.setDepartment(parent);
        } else {
            dept.setDepartment(null);
        }

        Employee previousHead = dept.getEmployee();
        if (request.getHeadEmployeeId() != null) {
            dept.setEmployee(findEmployeeInTenant(request.getHeadEmployeeId(), companyId));
        } else {
            dept.setEmployee(null);
        }

        // Only AnnouncementServiceImpl called NotificationService anywhere in this
        // slice - a department-head change (who now approves that department's
        // requests) told nobody it had happened.
        Employee newHead = dept.getEmployee();
        boolean headChanged = (previousHead == null) != (newHead == null)
                || (previousHead != null && newHead != null && !previousHead.getId().equals(newHead.getId()));
        if (headChanged && newHead != null && newHead.getUser() != null) {
            notificationService.send(com.businessos.shared.notification.CreateNotificationRequest.of(
                    com.businessos.enums.NotificationType.DEPARTMENT_HEAD_CHANGED,
                    "You're now head of " + dept.getName(),
                    "You have been assigned as head of the " + dept.getName() + " department.",
                    "/hrm/departments",
                    newHead.getUser().getId(),
                    companyId));
        }

        return DepartmentMapper.toResponse(dept);
    }

    @Override
    @Transactional
    public DepartmentResponse toggleActive(Long id) {
        authorizationService.checkPermission(PermissionCode.DEPARTMENT_UPDATE);
        Department dept = findInTenant(id);
        dept.setActive(!dept.isActive());
        return DepartmentMapper.toResponse(dept);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.DEPARTMENT_DELETE);
        Department dept = findInTenant(id);
        if (!dept.getEmployees().isEmpty()) {
            throw new BadRequestException(
                "Cannot delete a department that has employees. Reassign employees first.");
        }
        dept.softDelete();
    }

    private Department findInTenant(Long id) {
        return departmentRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Employee findEmployeeInTenant(Long employeeId, Long companyId) {
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        // The frontend picker already filters these out, but only the server can
        // actually enforce it - this was previously a UI-only exclusion.
        if (employee.getEmploymentStatus() != com.businessos.enums.EmploymentStatus.ACTIVE) {
            throw new BadRequestException(
                "Cannot set " + employee.getFullName() + " as department head - they are "
                    + employee.getEmploymentStatus() + ", not active");
        }
        return employee;
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }
}
