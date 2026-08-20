package com.businessos.modules.finance.fixedasset;

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
@RequestMapping("/api/company/finance/fixed-assets")
@RequiredArgsConstructor
@Tag(name = "Fixed Assets", description = "Capitalized assets with straight-line monthly depreciation")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class FixedAssetController {

    private final FixedAssetService assetService;

    @PostMapping
    @Operation(summary = "Register a fixed asset (optionally posting Dr Fixed Assets / Cr Cash)")
    public ResponseEntity<FixedAssetDtos.FixedAssetResponse> create(
            @Valid @RequestBody FixedAssetDtos.FixedAssetRequest request) {
        return new ResponseEntity<>(assetService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List fixed assets with book value and monthly depreciation")
    public ResponseEntity<Page<FixedAssetDtos.FixedAssetResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(assetService.list(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/{id}/dispose")
    @Operation(summary = "Dispose an asset (writes remaining book value off the books)")
    public ResponseEntity<FixedAssetDtos.FixedAssetResponse> dispose(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.dispose(id));
    }

    @PostMapping("/run-depreciation")
    @Operation(summary = "Run monthly straight-line depreciation for all active assets (idempotent per month)")
    public ResponseEntity<FixedAssetDtos.DepreciationRunResponse> runDepreciation(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(assetService.runDepreciation(year, month));
    }

    @GetMapping("/depreciation-runs")
    @Operation(summary = "History of executed depreciation runs")
    public ResponseEntity<List<FixedAssetDtos.DepreciationRunResponse>> listRuns() {
        return ResponseEntity.ok(assetService.listRuns());
    }
}
