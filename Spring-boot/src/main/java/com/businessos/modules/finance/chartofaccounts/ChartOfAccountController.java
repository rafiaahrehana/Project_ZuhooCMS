package com.businessos.modules.finance.chartofaccounts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company/finance/chart-of-accounts")
@RequiredArgsConstructor
@Tag(name = "Chart of Accounts", description = "Chart of Accounts Management")
public class ChartOfAccountController {

    private final ChartOfAccountService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Create Chart of Account")
    public ResponseEntity<ChartOfAccountResponse> create(@Valid @RequestBody ChartOfAccountRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get Chart of Account by ID")
    public ResponseEntity<ChartOfAccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get Chart of Account by Code")
    public ResponseEntity<ChartOfAccountResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get all Chart of Accounts")
    public ResponseEntity<Page<ChartOfAccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "accountCode") String sortBy) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get Chart of Accounts by Type")
    public ResponseEntity<Page<ChartOfAccountResponse>> getByType(
            @PathVariable AccountType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByType(type, PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Update Chart of Account")
    public ResponseEntity<ChartOfAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChartOfAccountRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @Operation(summary = "Delete Chart of Account")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
