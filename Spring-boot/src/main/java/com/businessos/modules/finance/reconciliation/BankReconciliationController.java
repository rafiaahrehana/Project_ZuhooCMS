package com.businessos.modules.finance.reconciliation;

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
@RequestMapping("/api/company/finance/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Bank Reconciliation", description = "Bank Reconciliation Management")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class BankReconciliationController {

    private final BankReconciliationService service;

    @PostMapping
    @Operation(summary = "Create a Bank Reconciliation Request")
    public ResponseEntity<BankReconciliationResponse> create(@Valid @RequestBody BankReconciliationRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Bank Reconciliation by ID")
    public ResponseEntity<BankReconciliationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all Bank Reconciliations")
    public ResponseEntity<Page<BankReconciliationResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/uncleared-transactions")
    @Operation(summary = "List GL transactions on this reconciliation's account that haven't cleared the bank yet")
    public ResponseEntity<List<com.businessos.modules.finance.generalledger.GeneralLedgerResponse>> unclearedTransactions(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getUnclearedTransactions(id));
    }

    @PostMapping("/{id}/transactions/{glEntryId}/toggle")
    @Operation(summary = "Mark a transaction as cleared (or un-cleared) against this reconciliation's bank statement")
    public ResponseEntity<BankReconciliationResponse> toggleTransaction(
            @PathVariable Long id,
            @PathVariable Long glEntryId,
            @RequestParam boolean cleared) {
        return ResponseEntity.ok(service.toggleTransactionCleared(id, glEntryId, cleared));
    }

    @PostMapping("/{id}/import-statement")
    @Operation(summary = "Import a bank-statement CSV (date, description, amount) and auto-clear matching transactions")
    public ResponseEntity<StatementImportResult> importStatement(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(service.importStatement(id, file));
    }

    @PostMapping("/{id}/statement")
    @Operation(summary = "Attach the bank statement file (upload it via /api/upload first) for audit trail")
    public ResponseEntity<BankReconciliationResponse> attachStatement(
            @PathVariable Long id, @Valid @RequestBody AttachStatementRequest request) {
        return ResponseEntity.ok(service.attachStatement(id, request));
    }

    @PostMapping("/{id}/reconcile")
    @Operation(summary = "Mark a Bank Reconciliation as Reconciled - fails if there's still an unexplained difference")
    public ResponseEntity<Void> markAsReconciled(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        service.markAsReconciled(id, notes);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all Pending Bank Reconciliations")
    public ResponseEntity<List<BankReconciliationResponse>> getPendingReconciliations() {
        return ResponseEntity.ok(service.getPendingReconciliations());
    }
}
