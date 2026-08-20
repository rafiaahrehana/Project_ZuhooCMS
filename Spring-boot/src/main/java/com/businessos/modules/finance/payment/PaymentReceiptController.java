package com.businessos.modules.finance.payment;

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

@RestController
@RequestMapping("/api/company/finance/payment-receipts")
@RequiredArgsConstructor
@Tag(name = "Payment Receipts", description = "Payment Receipt Management")
public class PaymentReceiptController {

    private final PaymentReceiptService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Create Payment Receipt")
    public ResponseEntity<PaymentReceiptResponse> create(@Valid @RequestBody PaymentReceiptRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get Payment Receipt by ID")
    public ResponseEntity<PaymentReceiptResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get the caller's own payment receipts")
    public ResponseEntity<Page<PaymentReceiptResponse>> getMyReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getMyReceipts(PageRequest.of(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get all Payment Receipts")
    public ResponseEntity<Page<PaymentReceiptResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Confirm Payment Receipt")
    public ResponseEntity<Void> confirmPayment(@PathVariable Long id) {
        service.confirmPayment(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Mark Payment Receipt as Deposited")
    public ResponseEntity<Void> markAsDeposited(
            @PathVariable Long id,
            @RequestParam String bank) {
        service.markAsDeposited(id, bank);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Reverse a confirmed payment (NSF/chargeback) - restores the invoice balance and unwinds the GL")
    public ResponseEntity<Void> reverse(@PathVariable Long id, @RequestParam(required = false) String reason) {
        service.reverse(id, reason);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @Operation(summary = "Delete Payment Receipt")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
