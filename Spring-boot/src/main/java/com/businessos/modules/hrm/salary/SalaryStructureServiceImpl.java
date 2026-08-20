package com.businessos.modules.hrm.salary;

import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public SalaryStructureResponse create(SalaryStructureRequest request) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        Long companyId = requireCompanyId();
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        // Expire any current active structure
        salaryStructureRepository.findByEmployeeIdAndEffectiveToIsNull(request.getEmployeeId())
            .ifPresent(existing -> {
                existing.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                salaryStructureRepository.save(existing);
            });

        SalaryStructure s = SalaryStructure.builder()
            .employee(employee)
            .company(companyRef(companyId))
            .effectiveFrom(request.getEffectiveFrom())
            .grossSalary(request.getGrossSalary())
            .basicSalary(request.getBasicSalary())
            .houseRent(orZero(request.getHouseRent()))
            .medicalAllowance(orZero(request.getMedicalAllowance()))
            .transportAllowance(orZero(request.getTransportAllowance()))
            .foodAllowance(orZero(request.getFoodAllowance()))
            .specialAllowance(orZero(request.getSpecialAllowance()))
            .providentFund(orZero(request.getProvidentFund()))
            .taxDeduction(orZero(request.getTaxDeduction()))
            .notes(request.getNotes())
            .approvedBy(securityUtil.getCurrentUser())
            .build();

        salaryStructureRepository.save(s);

        employee.setBasicSalary(request.getBasicSalary());
        employee.setHouseRent(orZero(request.getHouseRent()));
        employee.setMedicalAllowance(orZero(request.getMedicalAllowance()));
        employee.setTransportAllowance(orZero(request.getTransportAllowance()));
        employee.setSalaryStructure(s);
        employeeRepository.save(employee);

        // Best-effort notification — a failed email must NEVER roll back the salary
        // structure that was just saved. (Previously this re-threw, aborting the whole
        // @Transactional create, so structures silently never persisted.)
        if (employee.getUser() != null) {
            try {
                Company fullCompany = companyRepository.findById(companyId).orElse(null);
                if (fullCompany != null) {
                    EmailBranding.Data branding = emailBranding.from(fullCompany);
                    emailService.sendSalaryRevisionEmail(
                        employee.getUser().getEmail(), employee.getUser().getFirstName(), branding);
                }
            } catch (Exception ex) {
                log.warn("Salary revision email failed for employee {} (structure was still saved): {}",
                    employee.getId(), ex.getMessage());
            }
        }

        return SalaryStructureMapper.toSalaryStructureResponse(s);
    }

    @Override
    @Transactional
    public SalaryStructureResponse update(Long id, SalaryStructureRequest request) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        SalaryStructure s = findInTenant(id);

        // Only the current (active) structure may be edited. Superseded historical
        // structures are locked to preserve payroll history — supersede them with a
        // new structure instead.
        if (s.getEffectiveTo() != null) {
            throw new BadRequestException(
                "Only the current salary structure can be edited. Create a new one to supersede this.");
        }

        s.setEffectiveFrom(request.getEffectiveFrom());
        s.setGrossSalary(request.getGrossSalary());
        s.setBasicSalary(request.getBasicSalary());
        s.setHouseRent(orZero(request.getHouseRent()));
        s.setMedicalAllowance(orZero(request.getMedicalAllowance()));
        s.setTransportAllowance(orZero(request.getTransportAllowance()));
        s.setFoodAllowance(orZero(request.getFoodAllowance()));
        s.setSpecialAllowance(orZero(request.getSpecialAllowance()));
        s.setProvidentFund(orZero(request.getProvidentFund()));
        s.setTaxDeduction(orZero(request.getTaxDeduction()));
        s.setNotes(request.getNotes());
        salaryStructureRepository.save(s);

        // Keep the employee's denormalized salary fields in sync.
        Employee employee = s.getEmployee();
        if (employee != null) {
            employee.setBasicSalary(request.getBasicSalary());
            employee.setHouseRent(orZero(request.getHouseRent()));
            employee.setMedicalAllowance(orZero(request.getMedicalAllowance()));
            employee.setTransportAllowance(orZero(request.getTransportAllowance()));
            employeeRepository.save(employee);
        }

        return SalaryStructureMapper.toSalaryStructureResponse(s);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getById(Long id) {
        return SalaryStructureMapper.toSalaryStructureResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getActiveForEmployee(Long employeeId) {
        requireViewOrOwn(employeeId);
        SalaryStructure s = salaryStructureRepository.findByEmployeeIdAndEffectiveToIsNull(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("No active salary structure for employee: " + employeeId));
        return SalaryStructureMapper.toSalaryStructureResponse(s);
    }

    /**
     * Unlike listAll/listForEmployee (which check SALARY_STRUCTURE_VIEW),
     * these two per-employee lookups had no check at all - any authenticated
     * colleague could read anyone's exact basic salary, allowances, PF, and
     * full structure history just by supplying an employeeId. An employee
     * viewing their own structure is still allowed with no special
     * permission, same self-service carve-out used for payslips/attendance.
     */
    private void requireViewOrOwn(Long employeeId) {
        if (authorizationService.hasPermission(PermissionCode.SALARY_STRUCTURE_VIEW)) {
            return;
        }
        var currentUser = securityUtil.getCurrentUser();
        Employee me = currentUser != null ? employeeRepository.findByUserId(currentUser.getId()).orElse(null) : null;
        if (me == null || employeeId == null || !me.getId().equals(employeeId)) {
            throw new com.businessos.shared.exception.ForbiddenException(
                    "Access denied: you can only view your own salary structure");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalaryStructureResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        return salaryStructureRepository.findAllInCompany(requireCompanyId(), pageable)
            .map(SalaryStructureMapper::toSalaryStructureResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalaryStructureResponse> listForEmployee(Long employeeId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        return salaryStructureRepository.findByCompanyIdAndEmployeeId(requireCompanyId(), employeeId, pageable)
            .map(SalaryStructureMapper::toSalaryStructureResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> historyForEmployee(Long employeeId) {
        requireViewOrOwn(employeeId);
        return salaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)
            .stream().map(SalaryStructureMapper::toSalaryStructureResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_DELETE);
        SalaryStructure s = findInTenant(id);
        if (s.getEffectiveTo() == null) {
            throw new BadRequestException("Cannot delete the currently active salary structure. Supersede it by creating a new one.");
        }
        s.softDelete();
    }

    private SalaryStructure findInTenant(Long id) {
        return salaryStructureRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
