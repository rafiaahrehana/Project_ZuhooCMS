package com.businessos.modules.hrm.employee;

import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.designation.Designation;
import com.businessos.modules.hrm.attendance.shift.Shift;
import com.businessos.auth.user.User;
import com.businessos.enums.EmploymentStatus;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.enums.Role;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.department.DepartmentRepository;
import com.businessos.modules.hrm.designation.DesignationRepository;
import com.businessos.modules.hrm.attendance.shift.ShiftRepository;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.notification.NotificationPreferenceService;
import com.businessos.shared.notification.NotificationService;
import com.businessos.shared.notification.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final ShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationService notificationService;
    private final EmployeeMapper employeeMapper;
    private final com.businessos.shared.address.AddressMapper addressMapper;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null)
            throw new BadRequestException("No company context found in security context.");
        return companyId;
    }

    private Company findCompanyById(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
    }

    private Employee findEmployeeById(Long id) {
        Long companyId = requireCompanyId();
        return employeeRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private Employee findCurrentEmployee() {
        User user = securityUtil.getCurrentUser();
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found."));
    }

    private void validateEmployeeCreation(CreateEmployeeRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail))
            throw new BadRequestException("An account with this email already exists.");
    }

    private void validateNotSelfManager(Long employeeId, Long managerId) {
        if (managerId.equals(employeeId))
            throw new BadRequestException("An employee cannot be their own reporting manager.");
    }

    private User createPortalUser(CreateEmployeeRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)
                .active(true)
                .emailVerified(true)
                .image(request.getProfileImageUrl())
                .build();
        userRepository.save(user);

        return user;
    }

    private Employee buildEmployee(CreateEmployeeRequest request, User user, Company company) {
        return Employee.builder()
                .user(user)
                .company(company)
                .employeeNumber(EmployeeNumberGenerator.next(employeeRepository, company.getId()))
                .officialEmail(request.getOfficialEmail())
                .workPhone(request.getWorkPhone())
                .profileImageUrl(request.getProfileImageUrl())
                .nationalId(request.getNationalId())
                .taxId(request.getTaxId())
                .costCenter(request.getCostCenter())
                .officeLocation(request.getOfficeLocation())
                .jobTitle(request.getJobTitle())
                .employmentType(request.getEmploymentType())
                .employmentStatus(request.getEmploymentStatus() != null
                        ? request.getEmploymentStatus()
                        : EmploymentStatus.PROBATION)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .location(addressMapper.toEntity(request.getLocation()))
                .hireDate(request.getHireDate())
                .confirmationDate(request.getConfirmationDate())
                .probationEndDate(request.getProbationEndDate())
                .contractEndDate(request.getContractEndDate())
                .basicSalary(request.getBasicSalary())
                .houseRent(request.getHouseRent())
                .medicalAllowance(request.getMedicalAllowance())
                .transportAllowance(request.getTransportAllowance())
                .billableRate(request.getBillableRate())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankRoutingNumber(request.getBankRoutingNumber())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelation(request.getEmergencyContactRelation())
                .build();
    }

    private void assignEmployeeRelationships(Employee employee, CreateEmployeeRequest request, Long companyId) {
        if (request.getDepartmentId() != null)
            employee.setDepartment(findDepartmentById(request.getDepartmentId(), companyId));
        if (request.getDesignationId() != null)
            employee.setDesignation(findDesignationById(request.getDesignationId(), companyId));
        if (request.getReportingManagerId() != null)
            employee.setReportingManager(findReportingManagerById(request.getReportingManagerId(), companyId));
        if (request.getShiftId() != null)
            employee.setShift(findShiftById(request.getShiftId(), companyId));
    }

    private void sendWelcomeEmail(User user, Company company) {
        try {
            // company is already loaded — no redundant DB lookup needed
            EmailBranding.Data branding = emailBranding.from(company);
            emailService.sendEmployeeWelcomeEmail(user.getEmail(), user.getFirstName(), branding);
        } catch (Exception ex) {
            // Email failure must not fail employee creation — log and continue
            log.error("Welcome email failed for platformuser {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    private void updateEmployeeDetails(Employee emp, UpdateEmployeeRequest request) {
        if (request.getJobTitle() != null)
            emp.setJobTitle(request.getJobTitle());
        if (request.getEmploymentType() != null)
            emp.setEmploymentType(request.getEmploymentType());
        if (request.getEmploymentStatus() != null) {
            emp.setEmploymentStatus(request.getEmploymentStatus());
            // active drives payroll eligibility (findByCompanyIdAndActiveTrue) and
            // headcount counts, independently of employmentStatus - without this,
            // picking RESIGNED/TERMINATED/RETIRED/SUSPENDED from this ordinary edit
            // form (the obvious place an HR admin would record a departure) left
            // someone fully paid and portal-logged-in with a status that says
            // otherwise. dedicated terminate() below still does the fuller
            // soft-delete flow - this only keeps the two flags from diverging.
            if (isTerminalStatus(request.getEmploymentStatus())) {
                emp.setActive(false);
                deactivatePortalUser(emp);
            }
        }
        if (request.getGender() != null)
            emp.setGender(request.getGender());
        if (request.getDateOfBirth() != null)
            emp.setDateOfBirth(request.getDateOfBirth());
        if (request.getFatherName() != null)
            emp.setFatherName(request.getFatherName());
        if (request.getMotherName() != null)
            emp.setMotherName(request.getMotherName());
        if (request.getLocation() != null) {
            if (emp.getLocation() == null) {
                emp.setLocation(addressMapper.toEntity(request.getLocation()));
            } else {
                addressMapper.updateEntityFromRequest(emp.getLocation(), request.getLocation());
            }
        }
        if (request.getHireDate() != null)
            emp.setHireDate(request.getHireDate());
        if (request.getConfirmationDate() != null)
            emp.setConfirmationDate(request.getConfirmationDate());
        if (request.getProbationEndDate() != null)
            emp.setProbationEndDate(request.getProbationEndDate());
        if (request.getContractEndDate() != null) {
            emp.setContractEndDate(request.getContractEndDate());
            emp.setContractEndReminderSentAt(null);
        }
        if (request.getBasicSalary() != null)
            emp.setBasicSalary(request.getBasicSalary());
        if (request.getHouseRent() != null)
            emp.setHouseRent(request.getHouseRent());
        if (request.getMedicalAllowance() != null)
            emp.setMedicalAllowance(request.getMedicalAllowance());
        if (request.getTransportAllowance() != null)
            emp.setTransportAllowance(request.getTransportAllowance());
        if (request.getBillableRate() != null)
            emp.setBillableRate(request.getBillableRate());
        if (request.getBankName() != null)
            emp.setBankName(request.getBankName());
        if (request.getBankAccountNumber() != null)
            emp.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankRoutingNumber() != null)
            emp.setBankRoutingNumber(request.getBankRoutingNumber());
        if (request.getEmergencyContactName() != null)
            emp.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            emp.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelation() != null)
            emp.setEmergencyContactRelation(request.getEmergencyContactRelation());
        if (request.getNationalId() != null)
            emp.setNationalId(request.getNationalId());
        if (request.getTaxId() != null)
            emp.setTaxId(request.getTaxId());
        if (request.getCostCenter() != null)
            emp.setCostCenter(request.getCostCenter());
        if (request.getOfficeLocation() != null)
            emp.setOfficeLocation(request.getOfficeLocation());
        if (request.getWorkPhone() != null)
            emp.setWorkPhone(request.getWorkPhone());
        if (request.getOfficialEmail() != null)
            emp.setOfficialEmail(request.getOfficialEmail());
        if (request.getProfileImageUrl() != null) {
            emp.setProfileImageUrl(request.getProfileImageUrl());
            User user = emp.getUser();
            if (user != null) {
                user.setImage(request.getProfileImageUrl());
            }
        }
        if (request.getLocation() != null) {
            if (emp.getLocation() == null) {
                emp.setLocation(addressMapper.toEntity(request.getLocation()));
            } else {
                addressMapper.updateEntityFromRequest(emp.getLocation(), request.getLocation());
            }
        }
    }

    private void updateEmployeeRelationships(Employee emp, UpdateEmployeeRequest request, Long companyId) {
        if (request.getDepartmentId() != null)
            emp.setDepartment(findDepartmentById(request.getDepartmentId(), companyId));
        if (request.getDesignationId() != null)
            emp.setDesignation(findDesignationById(request.getDesignationId(), companyId));
        if (request.getReportingManagerId() != null) {
            validateNotSelfManager(emp.getId(), request.getReportingManagerId());
            emp.setReportingManager(findReportingManagerById(request.getReportingManagerId(), companyId));
        }
        if (request.getShiftId() != null)
            emp.setShift(findShiftById(request.getShiftId(), companyId));
    }

    /** RESIGNED/TERMINATED/RETIRED/SUSPENDED all mean "not currently working here" for payroll/portal purposes. */
    private boolean isTerminalStatus(EmploymentStatus status) {
        return status == EmploymentStatus.RESIGNED || status == EmploymentStatus.TERMINATED
                || status == EmploymentStatus.RETIRED || status == EmploymentStatus.SUSPENDED;
    }

    private void deactivatePortalUser(Employee emp) {
        User user = emp.getUser();
        if (user == null)
            return;
        user.setActive(false);
        user.softDelete();
        userRepository.save(user);
    }

    private Department findDepartmentById(Long departmentId, Long companyId) {
        return departmentRepository.findByIdAndCompanyId(departmentId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
    }

    private Designation findDesignationById(Long designationId, Long companyId) {
        return designationRepository.findByIdAndCompanyId(designationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + designationId));
    }

    private Shift findShiftById(Long shiftId, Long companyId) {
        return shiftRepository.findByIdAndCompanyId(shiftId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + shiftId));
    }

    private Employee findReportingManagerById(Long managerId, Long companyId) {
        return employeeRepository.findByIdAndCompanyId(managerId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporting manager not found with id: " + managerId));
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        authorizationService.checkPermission(PermissionCode.EMPLOYEE_CREATE);

        Long companyId = requireCompanyId();
        validateEmployeeCreation(request);
        Company company = findCompanyById(companyId);
        User user = createPortalUser(request);
        Employee employee = buildEmployee(request, user, company);
        assignEmployeeRelationships(employee, request, companyId);
        employeeRepository.save(employee);
        notificationPreferenceService.createDefaultsForUser(user.getId());
        sendWelcomeEmail(user, company);

        return employeeMapper.toDTO(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {

        return employeeMapper.toDTO(findEmployeeById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getMyProfile() {

        return employeeMapper.toDTO(findCurrentEmployee());
    }

    @Override
    @Transactional
    public EmployeeResponse updateMyProfile(SelfUpdateEmployeeRequest request) {
        Employee emp = findCurrentEmployee();

        if (request.getWorkPhone() != null)
            emp.setWorkPhone(request.getWorkPhone());
        if (request.getPhone() != null) {
            User user = emp.getUser();
            if (user != null) {
                user.setPhone(request.getPhone());
            }
        }
        if (request.getGender() != null)
            emp.setGender(request.getGender());
        if (request.getFatherName() != null)
            emp.setFatherName(request.getFatherName());
        if (request.getMotherName() != null)
            emp.setMotherName(request.getMotherName());
        if (request.getNationalId() != null)
            emp.setNationalId(request.getNationalId());
        if (request.getTaxId() != null)
            emp.setTaxId(request.getTaxId());
        if (request.getEmergencyContactName() != null)
            emp.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            emp.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelation() != null)
            emp.setEmergencyContactRelation(request.getEmergencyContactRelation());
        if (request.getLocation() != null) {
            if (emp.getLocation() == null) {
                emp.setLocation(addressMapper.toEntity(request.getLocation()));
            } else {
                addressMapper.updateEntityFromRequest(emp.getLocation(), request.getLocation());
            }
        }
        if (request.getProfileImageUrl() != null) {
            emp.setProfileImageUrl(request.getProfileImageUrl());
            User user = emp.getUser();
            if (user != null) {
                user.setImage(request.getProfileImageUrl());
            }
        }

        employeeRepository.save(emp);
        return employeeMapper.toDTO(emp);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listAll(Long departmentId, EmploymentStatus status, String search, boolean excludeOwner, Pageable pageable) {
        Long companyId = requireCompanyId();

        Long ownerUserId = null;
        if (excludeOwner) {
            try {
                Company company = findCompanyById(companyId);
                if (company != null && company.getOwner() != null) {
                    ownerUserId = company.getOwner().getId();
                }
            } catch (Exception ignored) {
                ownerUserId = null;
            }
        }

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null;
        boolean hasDept = departmentId != null;

        // 1. No search text active
        if (!hasSearch) {
            Page<Employee> page;
            if (hasDept && hasStatus) {
                page = employeeRepository.findByCompanyIdAndDepartmentIdAndEmploymentStatusExcludingOwner(companyId, departmentId, status, ownerUserId, pageable);
            } else if (hasDept) {
                page = employeeRepository.findByCompanyIdAndDepartmentIdExcludingOwner(companyId, departmentId, ownerUserId, pageable);
            } else if (hasStatus) {
                page = employeeRepository.findByCompanyIdAndEmploymentStatusExcludingOwner(companyId, status, ownerUserId, pageable);
            } else {
                page = employeeRepository.findByCompanyIdExcludingOwner(companyId, ownerUserId, pageable);
            }
            return page.map(employeeMapper::toDTO);
        }

        // 2. Search text active
        String searchKeyword = search.trim();
        Page<Employee> page;
        if (hasStatus) {
            page = employeeRepository.searchEmployeesWithStatus(
                    companyId, departmentId, status, ownerUserId, searchKeyword, pageable);
        } else {
            page = employeeRepository.searchEmployeesWithoutStatus(
                    companyId, departmentId, ownerUserId, searchKeyword, pageable);
        }
        return page.map(employeeMapper::toDTO);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        authorizationService.checkPermission(PermissionCode.EMPLOYEE_UPDATE);

        Long companyId = requireCompanyId();
        Employee emp = findEmployeeById(id);
        updateEmployeeDetails(emp, request);
        updateEmployeeRelationships(emp, request, companyId);
        employeeRepository.save(emp);

        return employeeMapper.toDTO(emp);
    }

    @Override
    @Transactional
    public void terminate(Long id) {
        authorizationService.checkPermission(PermissionCode.EMPLOYEE_DELETE);

        Employee emp = findEmployeeById(id);
        emp.setActive(false);
        emp.setEmploymentStatus(EmploymentStatus.TERMINATED);
        emp.softDelete();
        deactivatePortalUser(emp);

        if (emp.getUser() != null) {
            try {
                EmailBranding.Data branding = emailBranding.from(emp.getCompany());
                emailService.sendTerminationEmail(
                        emp.getUser().getEmail(), emp.getUser().getFirstName(), branding);
            } catch (Exception ex) {
                // Best-effort notification — a failed email must not roll back the termination.
                log.warn("Termination email failed for employee {} (termination still saved): {}",
                        emp.getId(), ex.getMessage());
            }
        }

        // Only the terminated employee themselves was ever told - grep-confirmed
        // AnnouncementServiceImpl was the only caller of NotificationService in
        // this whole slice (Employee/Department/Designation/Shift/Holiday).
        // Nobody was prompted to actually collect the badge/laptop or reassign
        // their work. Notify the reporting manager, or the owner if there isn't one.
        Employee manager = emp.getReportingManager();
        User recipient = manager != null ? manager.getUser() : null;
        if (recipient == null) recipient = emp.getCompany().getOwner();
        if (recipient != null) {
            notificationService.send(CreateNotificationRequest.of(
                    com.businessos.enums.NotificationType.EMPLOYEE_TERMINATED,
                    "Employee terminated",
                    emp.getFullName() + " has been terminated - reassign their work and confirm asset return.",
                    "/hrm/employees/" + emp.getId(),
                    recipient.getId(),
                    emp.getCompany().getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getEmployeeCount() {
        return employeeRepository.countByCompanyId(requireCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmployee(Long userId) {
        return employeeRepository.existsByUserIdAndCompanyId(userId, requireCompanyId());
    }
}
