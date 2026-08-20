package com.businessos.modules.hrm.attendance.biometric.data;
import com.businessos.modules.hrm.attendance.biometric.device.BiometricDevice;
import com.businessos.modules.hrm.attendance.biometric.device.BiometricDeviceRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeBiometricServiceImpl implements EmployeeBiometricService {

    private final EmployeeBiometricDataRepository biometricDataRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository deviceRepository;
    private final AuthorizationService authorizationService;
    private final SecurityUtil securityUtil;

    private void requireViewOrOwn(Long employeeId) {
        // employeeRepository.findById is scoped by the request-level Hibernate tenant
        // filter, so this only succeeds for an employeeId in the caller's own company -
        // needed because hasPermission() alone only reflects the caller's own role, not
        // which company the target employeeId belongs to.
        boolean sameTenantEmployee = employeeId != null && employeeRepository.findById(employeeId).isPresent();
        if (sameTenantEmployee && authorizationService.hasPermission(PermissionCode.BIOMETRIC_VIEW)) {
            return;
        }
        User currentUser = securityUtil.getCurrentUser();
        Employee currentEmployee = currentUser != null
                ? employeeRepository.findByUserId(currentUser.getId()).orElse(null)
                : null;
        if (currentEmployee == null || employeeId == null || !currentEmployee.getId().equals(employeeId)) {
            throw new ForbiddenException("Access denied: you can only access your own biometric enrollment");
        }
    }

    // EmployeeBiometricData has no company_id column/tenant filter of its own (unlike
    // Employee), so every lookup by the enrollment record's own id must explicitly verify
    // it belongs to the caller's company - otherwise any authenticated user could read or
    // mutate another company's biometric templates by guessing an id.
    private void requireSameTenant(EmployeeBiometricData data) {
        Long companyId = securityUtil.getCurrentCompanyId();
        Employee employee = data.getEmployee();
        if (companyId == null || employee == null || employee.getCompany() == null
                || !companyId.equals(employee.getCompany().getId())) {
            throw new ForbiddenException("Access denied: biometric enrollment belongs to a different company");
        }
    }

    @Override
    @Transactional
    public BiometricDataResponse enrollEmployee(BiometricEnrollmentRequest request) {
        authorizationService.checkPermission(PermissionCode.BIOMETRIC_MANAGE);
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        BiometricDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        // Check if at device capacity
        if (device.isAtCapacity()) {
            throw new RuntimeException("Device is at maximum enrollment capacity");
        }

        EmployeeBiometricData biometricData = EmployeeBiometricData.builder()
                .employee(employee)
                .device(device)
                .biometricType(request.getBiometricType())
                .biometricTemplate(request.getBiometricTemplate())
                .templateFormat(request.getTemplateFormat())
                .enrollmentDate(LocalDateTime.now())
                .enrollmentQualityScore(request.getQualityScore())
                .enrolled(true)
                .active(true)
                .build();

        biometricData = biometricDataRepository.save(biometricData);

        // Update device enrollment count
        device.setTotalEnrollments(device.getTotalEnrollments() + 1);
        deviceRepository.save(device);

        return BiometricDataMapper.toResponse(biometricData);
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricDataResponse getEnrollment(Long employeeId, Long deviceId) {
        EmployeeBiometricData data = biometricDataRepository
                .findByEmployeeIdAndDeviceIdAndBiometricType(employeeId, deviceId, "FINGERPRINT")
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        requireViewOrOwn(employeeId);
        return BiometricDataMapper.toResponse(data);
    }

    @Override
    @Transactional(readOnly = true)
    public BiometricDataResponse getById(Long id) {
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        requireViewOrOwn(data.getEmployee() != null ? data.getEmployee().getId() : null);
        return BiometricDataMapper.toResponse(data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiometricDataResponse> getByEmployee(Long employeeId) {
        requireViewOrOwn(employeeId);
        return biometricDataRepository.findByEmployeeId(employeeId)
                .stream()
                .map(BiometricDataMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean verifyBiometric(Long employeeId, Long deviceId, String template, double threshold) {
        EmployeeBiometricData data = biometricDataRepository
                .findByEmployeeIdAndDeviceIdAndBiometricType(employeeId, deviceId, "FINGERPRINT")
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        double matchScore = calculateMatch(data.getBiometricTemplate(), template);

        if (matchScore >= threshold) {
            updateLastVerified(data.getId());
            recordSuccessfulMatch(data.getId());
            return true;
        }
        recordFailedMatch(data.getId());
        return false;
    }

    @Override
    @Transactional
    public boolean verifyBiometric(Long id, String template, double threshold) {
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);

        double matchScore = calculateMatch(data.getBiometricTemplate(), template);

        if (matchScore >= threshold) {
            updateLastVerified(id);
            recordSuccessfulMatch(id);
            return true;
        }
        recordFailedMatch(id);
        return false;
    }

    @Override
    @Transactional
    public void updateEnrollmentStatus(Long id, boolean enrolled) {
        authorizationService.checkPermission(PermissionCode.BIOMETRIC_MANAGE);
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        data.setEnrolled(enrolled);
        biometricDataRepository.save(data);
    }

    @Override
    @Transactional
    public void updateLastVerified(Long id) {
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        data.setLastVerifiedTime(LocalDateTime.now());
        biometricDataRepository.save(data);
    }

    @Override
    @Transactional
    public void recordSuccessfulMatch(Long id) {
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        data.setSuccessfulMatches(data.getSuccessfulMatches() + 1);
        biometricDataRepository.save(data);
    }

    @Override
    @Transactional
    public void recordFailedMatch(Long id) {
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        data.setFailedMatches(data.getFailedMatches() + 1);
        biometricDataRepository.save(data);
    }

    @Override
    @Transactional
    public BiometricDataResponse delete(Long id) {
        authorizationService.checkPermission(PermissionCode.BIOMETRIC_MANAGE);
        EmployeeBiometricData data = biometricDataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        requireSameTenant(data);
        data.softDelete();
        biometricDataRepository.save(data);
        return BiometricDataMapper.toResponse(data);
    }

    private double calculateMatch(String template1, String template2) {
        if (template1 == null || template2 == null) return 0.0;

        // Simple comparison (in production use actual fingerprint matching)
        int matches = 0;
        int total = Math.min(template1.length(), template2.length());

        for (int i = 0; i < total; i++) {
            if (template1.charAt(i) == template2.charAt(i)) {
                matches++;
            }
        }

        return total > 0 ? (matches * 100.0 / total) : 0.0;
    }
}

