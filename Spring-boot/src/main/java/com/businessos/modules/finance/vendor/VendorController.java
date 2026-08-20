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

import java.util.List;

@RestController
@RequestMapping("/api/company/finance/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor / supplier master records (Accounts Payable)")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @Operation(summary = "Create a vendor")
    public ResponseEntity<VendorDtos.VendorResponse> create(@Valid @RequestBody VendorDtos.VendorRequest request) {
        return new ResponseEntity<>(vendorService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vendor")
    public ResponseEntity<VendorDtos.VendorResponse> update(@PathVariable Long id,
            @Valid @RequestBody VendorDtos.VendorRequest request) {
        return ResponseEntity.ok(vendorService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activate/deactivate a vendor")
    public ResponseEntity<VendorDtos.VendorResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.toggle(id));
    }

    @GetMapping
    @Operation(summary = "List vendors (optional name search)")
    public ResponseEntity<Page<VendorDtos.VendorResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(vendorService.list(search, PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/active")
    @Operation(summary = "All active vendors - for dropdowns")
    public ResponseEntity<List<VendorDtos.VendorResponse>> listActive() {
        return ResponseEntity.ok(vendorService.listActive());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a vendor (only if it has no bills)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
