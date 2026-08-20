package com.businessos.modules.support.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/support/categories")
@RequiredArgsConstructor
@Tag(name = "Support Categories", description = "Support Category Management")
public class SupportCategoryController {

    private final SupportCategoryService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_MANAGER')")
    @Operation(summary = "Create Category")
    public ResponseEntity<SupportCategoryResponse> create(@Valid @RequestBody SupportCategoryRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Category by ID")
    public ResponseEntity<SupportCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Category by Name")
    public ResponseEntity<SupportCategoryResponse> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.getByName(name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get all Categories")
    public ResponseEntity<Page<SupportCategoryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Active Categories")
    public ResponseEntity<List<SupportCategoryResponse>> getActive() {
        return ResponseEntity.ok(service.getActive());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_MANAGER')")
    @Operation(summary = "Update Category")
    public ResponseEntity<SupportCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportCategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_MANAGER')")
    @Operation(summary = "Update Category Status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        service.updateStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_MANAGER')")
    @Operation(summary = "Delete Category")
    public ResponseEntity<SupportCategoryResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
