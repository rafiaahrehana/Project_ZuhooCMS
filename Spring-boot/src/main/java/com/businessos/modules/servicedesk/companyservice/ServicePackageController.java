package com.businessos.modules.servicedesk.companyservice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.businessos.enums.SubscriptionStatus;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/packages")
public class ServicePackageController {

    private final ServicePackageService packageService;

    // ── Package catalog ───────────────────────────────────────────

    /**
     * ADMIN creates a new service package in the company catalog.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ServicePackageResponse> create(
            @Valid @RequestBody ServicePackageRequest request) {
        return new ResponseEntity<>(packageService.create(request), HttpStatus.CREATED);
    }

    /**
     * ADMIN/STAFF list all packages (includes inactive).
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<ServicePackageResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(packageService.listAll(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /**
     * Any authenticated platformuser can browse the active package catalog.
     */
    @GetMapping("/active")
    public ResponseEntity<List<ServicePackageResponse>> listActive() {
        return ResponseEntity.ok(packageService.listActive());
    }

    /**
     * Get a single package by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getById(id));
    }

    /**
     * ADMIN updates package details and included services.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ServicePackageRequest request) {
        return ResponseEntity.ok(packageService.update(id, request));
    }

    /**
     * ADMIN toggles a package active/inactive.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ServicePackageResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.toggleActive(id));
    }

    /**
     * ADMIN soft-deletes a package (only if no active subscriptions).
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        packageService.delete(id);
        return ResponseEntity.ok("Package deleted successfully");
    }

    // ── Subscriptions ─────────────────────────────────────────────

    /**
     * CLIENT subscribes themselves to a package.
     * ADMIN/STAFF can subscribe on behalf of a client by providing clientId in body.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<PackageSubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        return new ResponseEntity<>(packageService.subscribe(request), HttpStatus.CREATED);
    }

    /**
     * ADMIN activates a PENDING_PAYMENT subscription (simulates payment confirmation).
     * In production, this would be called by a payment webhook.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/subscriptions/{id}/activate")
    public ResponseEntity<PackageSubscriptionResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.activate(id));
    }

    /**
     * ADMIN suspends an active subscription (e.g. overdue payment).
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/subscriptions/{id}/suspend")
    public ResponseEntity<PackageSubscriptionResponse> suspend(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(packageService.suspend(id, reason));
    }

    /**
     * ADMIN reactivates a suspended subscription.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/subscriptions/{id}/reactivate")
    public ResponseEntity<PackageSubscriptionResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.reactivate(id));
    }

    /**
     * ADMIN or CLIENT cancels a subscription.
     */
    @PatchMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<PackageSubscriptionResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(packageService.cancel(id, reason));
    }

    /**
     * Get a single subscription by ID.
     */
    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<PackageSubscriptionResponse> getSubscriptionById(
            @PathVariable Long id) {
        return ResponseEntity.ok(packageService.getSubscriptionById(id));
    }

    /**
     * ADMIN/STAFF list all subscriptions for the company, optionally filtered by status.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/subscriptions")
    public ResponseEntity<Page<PackageSubscriptionResponse>> listSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(packageService.listSubscriptions(status,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    /**
     * CLIENT lists their own subscriptions.
     */
    @GetMapping("/subscriptions/my")
    public ResponseEntity<Page<PackageSubscriptionResponse>> listMySubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(packageService.listMySubscriptions(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
