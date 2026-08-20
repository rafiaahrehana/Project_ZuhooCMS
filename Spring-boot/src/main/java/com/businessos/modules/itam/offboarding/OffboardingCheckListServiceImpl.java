package com.businessos.modules.itam.offboarding;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.asset.AssetRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.itam.software.SoftwareLicenseSeatRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.enums.NotificationType;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OffboardingCheckListServiceImpl implements OffboardingChecklistService {

    private final OffboardingChecklistRepository checklistRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
    private final SoftwareLicenseSeatRepository softwareLicenseSeatRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public OffboardingChecklistResponse create(OffboardingChecklistRequest request) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_CREATE);
        Long companyId = requireCompanyId();

        // Tenant-scoped employee fetch
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found or doesn't belong to your company"));

        // Check if one already exists
        if (checklistRepository.findByEmployeeIdAndCompanyId(employee.getId(), companyId).isPresent()) {
            throw new BadRequestException("Offboarding checklist already exists for this employee");
        }

        OffboardingChecklist checklist = OffboardingChecklist.builder()
                .companyId(companyId)
                .employee(employee)
                .offboardingDate(LocalDate.now())
                .hardwareCollected(false)
                .licensesRevoked(false)
                .accessRevoked(false)
                .dataHandedOver(false)
                .exitInterviewCompleted(false)
                .overallNotes(request.getNotes())
                .build();

        checklist = checklistRepository.save(checklist);
        notifyCreated(checklist, employee, companyId);
        return OffboardingChecklistMapper.toResponse(checklist);
    }

    // Previously nobody was told an offboarding started - IT/HR only found out
    // by opening the checklist list themselves, so asset/license collection
    // could sit untouched with no prompt to act.
    private void notifyCreated(OffboardingChecklist checklist, Employee employee, Long companyId) {
        Employee manager = employee.getReportingManager();
        User recipient = manager != null ? manager.getUser() : null;
        if (recipient == null) {
            Company company = companyRepository.findById(companyId).orElse(null);
            recipient = company != null ? company.getOwner() : null;
        }
        if (recipient == null) return;

        notificationService.send(CreateNotificationRequest.of(
                NotificationType.OFFBOARDING_CREATED,
                "Offboarding checklist started",
                (employee.getUser() != null ? employee.getUser().getFullName() : "An employee")
                        + " has an offboarding checklist to complete",
                "/itam/offboarding",
                recipient.getId(),
                companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public OffboardingChecklistResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_VIEW);
        return OffboardingChecklistMapper.toResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OffboardingChecklistResponse getByEmployee(Long employeeId) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_VIEW);
        Long companyId = requireCompanyId();
        OffboardingChecklist checklist = checklistRepository.findByEmployeeIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Offboarding checklist not found"));
        return OffboardingChecklistMapper.toResponse(checklist);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OffboardingChecklistResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_VIEW);
        Long companyId = requireCompanyId();
        return checklistRepository.findByCompanyId(companyId, pageable)
                .map(OffboardingChecklistMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OffboardingChecklistResponse> getPendingChecklists() {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_VIEW);
        Long companyId = requireCompanyId();
        // BUG FIX: previously called checklistRepository.findByCompleted(false) which wasn't defined and lacked tenant isolation
        return checklistRepository.findByCompanyIdAndCompletedFalse(companyId)
                .stream()
                .map(OffboardingChecklistMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markHardwareCollected(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_MANAGE);
        OffboardingChecklist checklist = findInTenant(id);

        long stillAssigned = assetRepository
                .findByCompanyIdAndAssignedToId(checklist.getCompanyId(), checklist.getEmployee().getId())
                .size();
        if (stillAssigned > 0) {
            throw new BadRequestException(
                    "Cannot mark hardware collected - " + stillAssigned + " asset(s) are still assigned to this employee. Unassign them first.");
        }

        checklist.setHardwareCollected(true);
        checklist.setHardwareCollectedDate(LocalDate.now());
        checklist.setHardwareCollectedBy(currentUserName());
        if (notes != null && !notes.isBlank()) checklist.setHardwareNotes(notes);
        checkCompletionStatus(checklist);
        checklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public void markLicensesRevoked(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_MANAGE);
        OffboardingChecklist checklist = findInTenant(id);

        long stillHeld = softwareLicenseSeatRepository
                .findByEmployeeIdAndCompanyIdAndReleasedAtIsNull(checklist.getEmployee().getId(), checklist.getCompanyId())
                .size();
        if (stillHeld > 0) {
            throw new BadRequestException(
                    "Cannot mark licenses revoked - " + stillHeld + " license seat(s) are still held by this employee. Release them first.");
        }

        checklist.setLicensesRevoked(true);
        checklist.setLicensesRevokedDate(LocalDate.now());
        if (notes != null && !notes.isBlank()) checklist.setLicensesNotes(notes);
        checkCompletionStatus(checklist);
        checklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public void markAccessRevoked(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_MANAGE);
        OffboardingChecklist checklist = findInTenant(id);
        checklist.setAccessRevoked(true);
        checklist.setAccessRevokedDate(LocalDate.now());
        if (notes != null && !notes.isBlank()) checklist.setAccessNotes(notes);
        checkCompletionStatus(checklist);
        checklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public void markDataHandedOver(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_MANAGE);
        OffboardingChecklist checklist = findInTenant(id);
        checklist.setDataHandedOver(true);
        checklist.setDataHandoverDate(LocalDate.now());
        if (notes != null && !notes.isBlank()) checklist.setDataHandoverNotes(notes);
        checkCompletionStatus(checklist);
        checklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public void markExitInterviewCompleted(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_MANAGE);
        OffboardingChecklist checklist = findInTenant(id);
        checklist.setExitInterviewCompleted(true);
        checklist.setExitInterviewDate(LocalDate.now());
        if (notes != null && !notes.isBlank()) checklist.setExitInterviewNotes(notes);
        checkCompletionStatus(checklist);
        checklistRepository.save(checklist);
    }

    @Override
    @Transactional
    public OffboardingChecklistResponse delete(Long id) {
        authorizationService.checkPermission(PermissionCode.OFFBOARDING_DELETE);
        OffboardingChecklist checklist = findInTenant(id);
        checklist.softDelete();
        checklistRepository.save(checklist);
        return OffboardingChecklistMapper.toResponse(checklist);
    }

    private void checkCompletionStatus(OffboardingChecklist checklist) {
        if (checklist.isAllTasksCompleted() && !checklist.isCompleted()) {
            checklist.setCompleted(true);
            checklist.setCompletionDate(java.time.LocalDate.now());
            checklist.setCompletedBy(currentUserName());
        }
    }

    private String currentUserName() {
        var user = securityUtil.getCurrentUser();
        return user != null ? user.getFullName() : null;
    }

    private OffboardingChecklist findInTenant(Long id) {
        return checklistRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found"));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) {
            throw new BadRequestException("No company context found in security token");
        }
        return id;
    }
}
