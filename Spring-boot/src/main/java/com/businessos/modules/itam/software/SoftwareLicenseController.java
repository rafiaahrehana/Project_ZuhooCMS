package com.businessos.modules.itam.software;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/itam/software")
public class SoftwareLicenseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SoftwareLicenseService licenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<SoftwareLicenseResponse> create(@Valid @RequestBody SoftwareLicenseRequest request) {
        return new ResponseEntity<>(licenseService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<SoftwareLicenseResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(licenseService.getAll(
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<SoftwareLicenseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(licenseService.getById(id));
    }

    @GetMapping("/key/{licenseKey}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<SoftwareLicenseResponse> getByKey(@PathVariable String licenseKey) {
        return ResponseEntity.ok(licenseService.getByLicenseKey(licenseKey));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<SoftwareLicenseResponse>> getByStatus(
            @PathVariable LicenseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(licenseService.getByStatus(status, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<SoftwareLicenseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SoftwareLicenseRequest request) {
        return ResponseEntity.ok(licenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        licenseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<SoftwareLicenseResponse>> getExpiringLicenses() {
        return ResponseEntity.ok(licenseService.getExpiringLicenses());
    }

    @GetMapping("/expired")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<SoftwareLicenseResponse>> getExpiredLicenses() {
        return ResponseEntity.ok(licenseService.getExpiredLicenses());
    }

    @PostMapping("/{id}/assign-seat")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> assignSeat(@PathVariable Long id, @RequestParam Long employeeId) {
        licenseService.assignSeat(id, employeeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/release-seat")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> releaseSeat(@PathVariable Long id, @RequestParam Long employeeId) {
        licenseService.releaseSeat(id, employeeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/seats")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get current seat holders for a license")
    public ResponseEntity<List<SoftwareLicenseSeatResponse>> getSeatHolders(@PathVariable Long id) {
        return ResponseEntity.ok(licenseService.getSeatHolders(id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get licenses currently assigned to an employee - used by offboarding")
    public ResponseEntity<List<SoftwareLicenseSeatResponse>> getLicensesForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(licenseService.getLicensesForEmployee(employeeId));
    }
}