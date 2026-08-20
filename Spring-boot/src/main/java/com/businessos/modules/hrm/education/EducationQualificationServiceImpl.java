package com.businessos.modules.hrm.education;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationQualificationServiceImpl implements EducationQualificationService {

    private final EducationQualificationRepository qualificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public EducationQualificationResponse create(EducationQualificationRequest request) {
        Long companyId = requireCompanyId();
        if (!authorizationService.hasPermission(PermissionCode.EMPLOYEE_UPDATE)) {
            requireOwnEmployee(request.getEmployeeId());
        }
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        EducationQualification qualification = EducationQualification.builder()
            .employee(employee)
            .company(companyRef(companyId))
            .degree(request.getDegree())
            .institution(request.getInstitution())
            .fieldOfStudy(request.getFieldOfStudy())
            .passingYear(request.getPassingYear())
            .result(request.getResult())
            .notes(request.getNotes())
            .build();

        qualificationRepository.save(qualification);
        return EducationQualificationMapper.toResponse(qualification);
    }

    @Override
    @Transactional
    public EducationQualificationResponse update(Long id, EducationQualificationRequest request) {
        EducationQualification qualification = findInTenant(id);
        if (!authorizationService.hasPermission(PermissionCode.EMPLOYEE_UPDATE)) {
            requireOwnEmployee(qualification.getEmployee().getId());
        }
        qualification.setDegree(request.getDegree());
        qualification.setInstitution(request.getInstitution());
        qualification.setFieldOfStudy(request.getFieldOfStudy());
        qualification.setPassingYear(request.getPassingYear());
        qualification.setResult(request.getResult());
        qualification.setNotes(request.getNotes());
        return EducationQualificationMapper.toResponse(qualification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EducationQualificationResponse> listForEmployee(Long employeeId) {
        if (!authorizationService.hasPermission(PermissionCode.EMPLOYEE_VIEW)) {
            requireOwnEmployee(employeeId);
        }
        return qualificationRepository
            .findByCompanyIdAndEmployeeIdOrderByPassingYearDesc(requireCompanyId(), employeeId)
            .stream().map(EducationQualificationMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EducationQualification qualification = findInTenant(id);
        if (!authorizationService.hasPermission(PermissionCode.EMPLOYEE_UPDATE)) {
            requireOwnEmployee(qualification.getEmployee().getId());
        }
        qualification.softDelete();
    }

    private EducationQualification findInTenant(Long id) {
        return qualificationRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Education qualification not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }

    // COMPANY_OWNER and anyone with EMPLOYEE_UPDATE/EMPLOYEE_VIEW can manage any
    // employee's qualifications; everyone else may only touch their own record
    // (mirrors the self-ownership fallback used by Expense/Leave elsewhere).
    private void requireOwnEmployee(Long employeeId) {
        User currentUser = securityUtil.getCurrentUser();
        Employee currentEmployee = currentUser != null
                ? employeeRepository.findByUserId(currentUser.getId()).orElse(null)
                : null;
        if (currentEmployee == null || employeeId == null || !currentEmployee.getId().equals(employeeId)) {
            throw new ForbiddenException("Access denied: you can only manage your own education qualifications");
        }
    }
}
