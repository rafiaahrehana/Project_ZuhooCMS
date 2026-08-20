package com.businessos.modules.finance.generalledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/company/finance/general-ledger")
@RequiredArgsConstructor
@Tag(name = "General Ledger", description = "General Ledger Management")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class GeneralLedgerController {

    private final GeneralLedgerService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get General Ledger Entry by ID")
    public ResponseEntity<GeneralLedgerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all General Ledger Entries")
    public ResponseEntity<Page<GeneralLedgerResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get General Ledger Entries by Account")
    public ResponseEntity<Page<GeneralLedgerResponse>> getByAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByAccount(accountId, PageRequest.of(page, size)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get General Ledger Entries by Date Range")
    public ResponseEntity<Page<GeneralLedgerResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByDateRange(start, end, PageRequest.of(page, size)));
    }

    @GetMapping("/reference")
    @Operation(summary = "Get General Ledger Entries by Reference")
    public ResponseEntity<List<GeneralLedgerResponse>> getByReference(
            @RequestParam GlReferenceType referenceType,
            @RequestParam Long referenceId) {
        return ResponseEntity.ok(service.getByReference(referenceType, referenceId));
    }

    @GetMapping("/account/{accountId}/balance")
    @Operation(summary = "Get Chart of Account Balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long accountId) {
        return ResponseEntity.ok(service.getAccountBalance(accountId));
    }

    @PostMapping("/{id}/reconcile")
    @Operation(summary = "Reconcile a General Ledger Entry")
    public ResponseEntity<Void> reconcile(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        service.reconcile(id, notes);
        return ResponseEntity.ok().build();
    }
}
