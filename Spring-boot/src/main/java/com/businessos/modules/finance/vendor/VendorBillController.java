package com.businessos.modules.finance.vendor;

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

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/company/finance/vendor-bills")
@RequiredArgsConstructor
@Tag(name = "Vendor Bills", description = "Accounts Payable - bills received from vendors")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class VendorBillController {

    private final VendorBillService billService;

    @PostMapping
    @Operation(summary = "Enter a vendor bill (DRAFT)")
    public ResponseEntity<VendorBillDtos.VendorBillResponse> create(
            @Valid @RequestBody VendorBillDtos.VendorBillRequest request) {
        return new ResponseEntity<>(billService.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a bill - recognizes the expense and the payable in the GL (different user than creator)")
    public ResponseEntity<VendorBillDtos.VendorBillResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(billService.approve(id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Record a (partial) payment against an approved bill - Dr AP / Cr Cash")
    public ResponseEntity<VendorBillDtos.VendorBillResponse> pay(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(billService.recordPayment(id, amount));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a bill (reverses the GL posting if it was approved; blocked once payments exist)")
    public ResponseEntity<VendorBillDtos.VendorBillResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(billService.cancel(id));
    }

    @GetMapping
    @Operation(summary = "List bills, optionally filtered by status or vendor")
    public ResponseEntity<Page<VendorBillDtos.VendorBillResponse>> list(
            @RequestParam(required = false) VendorBillStatus status,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billService.list(status, vendorId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/ap-ageing")
    @Operation(summary = "Accounts Payable ageing - what we owe each vendor, bucketed by days overdue")
    public ResponseEntity<VendorBillDtos.ApAgeingReport> apAgeing(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(billService.apAgeing(asOfDate));
    }
}
