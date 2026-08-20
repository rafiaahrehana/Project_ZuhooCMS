package com.businessos.modules.hrm.payroll.settings;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payroll policy for the caller's own company.
 *
 * Reading is open to anyone who can see payroll, because a payslip is not
 * explicable without knowing the divisor behind it. Changing it takes
 * COMPANY_SETTINGS - these values move real money for every employee.
 */
@RestController
@RequestMapping("/api/hr/payroll-settings")
@RequiredArgsConstructor
public class PayrollSettingsController {

    private final PayrollSettingsService service;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<PayrollSettings> get() {
        authorizationService.checkAnyPermission(PermissionCode.PAYROLL_VIEW, PermissionCode.COMPANY_SETTINGS);
        return ResponseEntity.ok(service.getOrCreateForCurrentCompany());
    }

    @PutMapping
    public ResponseEntity<PayrollSettings> update(@Valid @RequestBody PayrollSettingsRequest request) {
        authorizationService.checkPermission(PermissionCode.COMPANY_SETTINGS);
        return ResponseEntity.ok(service.update(request));
    }
}
