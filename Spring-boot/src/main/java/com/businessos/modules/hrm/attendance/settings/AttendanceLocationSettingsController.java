package com.businessos.modules.hrm.attendance.settings;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Office-location policy for the caller's own company.
 *
 * Reading is open to anyone who can see attendance, because a flagged
 * check-in is not explicable without knowing the radius behind it. Changing
 * it takes COMPANY_SETTINGS - it decides whether every employee's check-in
 * gets flagged for review.
 */
@RestController
@RequestMapping("/api/hr/attendance-location-settings")
@RequiredArgsConstructor
public class AttendanceLocationSettingsController {

    private final AttendanceLocationSettingsService service;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<AttendanceLocationSettings> get() {
        authorizationService.checkAnyPermission(PermissionCode.ATTENDANCE_VIEW, PermissionCode.COMPANY_SETTINGS);
        return ResponseEntity.ok(service.getOrCreateForCurrentCompany());
    }

    @PutMapping
    public ResponseEntity<AttendanceLocationSettings> update(@Valid @RequestBody AttendanceLocationSettingsRequest request) {
        authorizationService.checkPermission(PermissionCode.COMPANY_SETTINGS);
        return ResponseEntity.ok(service.update(request));
    }
}
