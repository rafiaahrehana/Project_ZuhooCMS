package com.businessos.modules.hrm.attendance.biometric.data;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company/biometric")
public class BiometricDataController {

    private final EmployeeBiometricService biometricService;

    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<BiometricDataResponse> enrollEmployee(@Valid @RequestBody BiometricEnrollmentRequest request) {
        return new ResponseEntity<>(biometricService.enrollEmployee(request), HttpStatus.CREATED);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<BiometricDataResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(biometricService.getByEmployee(employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<BiometricDataResponse> getEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(biometricService.getById(id));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Boolean> verifyBiometric(
            @PathVariable Long id,
            @RequestParam String template,
            @RequestParam(defaultValue = "95") double threshold) {
        return ResponseEntity.ok(biometricService.verifyBiometric(id, template, threshold));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        biometricService.delete(id);
        return ResponseEntity.noContent().build();
    }
}