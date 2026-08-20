package com.businessos.modules.hrm.designation;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.department.DepartmentRepository;
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

public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;
    private final com.businessos.modules.hrm.employee.EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public DesignationResponse create(DesignationRequest request) {
        authorizationService.checkPermission(PermissionCode.DESIGNATION_CREATE);
        Long companyId = requireCompanyId();
        if (designationRepository.existsByCompanyIdAndCode(companyId, request.getCode().toUpperCase())) {
            throw new BadRequestException("Designation code '" + request.getCode() + "' already exists");
        }
        Designation d = Designation.builder()
            .name(request.getName())
            .code(request.getCode().toUpperCase())
            .level(request.getLevel())
            .description(request.getDescription())
            .employmentCategory(request.getEmploymentCategory())
            .department(resolveDepartment(request.getDepartmentId(), companyId))
            .active(request.getActive() != null ? request.getActive() : true)
            .company(companyRef(companyId))
            .build();
        designationRepository.save(d);
        return DesignationMapper.toDesignationResponse(d);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse getById(Long id) {
        return DesignationMapper.toDesignationResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DesignationResponse> listAll(Pageable pageable) {
        // Deliberately NOT gated by DESIGNATION_VIEW: same reasoning as
        // DepartmentServiceImpl.listAll() - this endpoint doubles as a cross-module
        // picker. Frontend sidebar/route gating only, until the endpoint is split.
        return designationRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(DesignationMapper::toDesignationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> listActive() {
        return designationRepository.findByCompanyIdAndActiveTrueOrderByLevelAsc(requireCompanyId())
            .stream().map(DesignationMapper::toDesignationResponse).toList();
    }

    @Override
    @Transactional
    public DesignationResponse update(Long id, DesignationRequest request) {
        authorizationService.checkPermission(PermissionCode.DESIGNATION_UPDATE);
        Long companyId = requireCompanyId();
        Designation d = findInTenant(id);
        if (!d.getCode().equals(request.getCode().toUpperCase())
                && designationRepository.existsByCompanyIdAndCodeAndIdNot(
                    companyId, request.getCode().toUpperCase(), id)) {
            throw new BadRequestException("Designation code '" + request.getCode() + "' already exists");
        }
        d.setName(request.getName());
        d.setCode(request.getCode().toUpperCase());
        d.setLevel(request.getLevel());
        if (request.getDescription() != null) d.setDescription(request.getDescription());
        d.setEmploymentCategory(request.getEmploymentCategory());
        d.setDepartment(resolveDepartment(request.getDepartmentId(), companyId));
        if (request.getActive() != null) d.setActive(request.getActive());
        return DesignationMapper.toDesignationResponse(d);
    }

    private Department resolveDepartment(Long departmentId, Long companyId) {
        if (departmentId == null) return null;
        return departmentRepository.findByIdAndCompanyId(departmentId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
    }

    @Override
    @Transactional
    public DesignationResponse toggleActive(Long id) {
        authorizationService.checkPermission(PermissionCode.DESIGNATION_UPDATE);
        Designation d = findInTenant(id);
        d.setActive(!d.isActive());
        return DesignationMapper.toDesignationResponse(d);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.DESIGNATION_DELETE);
        Designation designation = findInTenant(id);
        // Department already guards this (delete():139-147); Designation and Shift
        // didn't - deleting one still referenced by employees silently disappeared
        // it from every screen and report showing that employee's designation.
        long assignedEmployees = employeeRepository.countByDesignationId(id);
        if (assignedEmployees > 0) {
            throw new BadRequestException(
                "Cannot delete this designation: it is currently assigned to " + assignedEmployees
                    + " employee(s). Reassign them first.");
        }
        designation.softDelete();
    }

    private Designation findInTenant(Long id) {
        return designationRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Designation not found: " + id));
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
