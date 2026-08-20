package com.businessos.modules.finance.expense;

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

@RestController("platformFinanceExpenseController")
@RequestMapping("/api/platform/finance/expenses")
@RequiredArgsConstructor
@Tag(name = "Platform Expenses", description = "Platform Expense Management")
public class PlatformExpenseController {

    private final ExpenseService service;

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Platform Expense")
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Platform Expense by ID")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all Platform Expenses")
    public ResponseEntity<Page<ExpenseResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Platform Expenses by Status")
    public ResponseEntity<Page<ExpenseResponse>> getByStatus(
            @PathVariable ExpenseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/vendor/{vendorName}")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Platform Expenses by Vendor")
    public ResponseEntity<Page<ExpenseResponse>> getByVendor(
            @PathVariable String vendorName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByVendorName(vendorName, PageRequest.of(page, size)));
    }

    @GetMapping("/my-expenses")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get My Platform Expenses")
    public ResponseEntity<Page<ExpenseResponse>> getMyExpenses(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getMyExpenses(employeeId, PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Platform Expense")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve Platform Expense")
    public ResponseEntity<Void> approve(
            @PathVariable Long id,
            @RequestParam String notes) {
        service.approveExpense(id, notes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reject Platform Expense")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestParam String reason) {
        service.rejectExpense(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/mark-as-paid")
    @PreAuthorize("hasRole('PLATFORM_ACCOUNTANT') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Mark Platform Expense as Paid")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String reimbursementMethod,
            @RequestParam(required = false) String referenceNumber) {
        service.markAsPaid(id, reimbursementMethod, referenceNumber);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete Platform Expense")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
