package com.businessos.modules.hrm.asset;

import jakarta.validation.Valid;

import com.businessos.enums.AssetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/assets")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return new ResponseEntity<>(assetService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<AssetResponse>> listAll(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(assetService.listAll(status, PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AssetResponse>> listForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(assetService.listForEmployee(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.update(id, request));
    }

    @PatchMapping("/{id}/assign/{employeeId}")
    public ResponseEntity<AssetResponse> assign(
            @PathVariable Long id,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(assetService.assign(id, employeeId));
    }

    @PatchMapping("/{id}/unassign")
    public ResponseEntity<AssetResponse> unassign(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.unassign(id));
    }

    @PatchMapping("/{id}/maintenance")
    public ResponseEntity<AssetResponse> setMaintenance(
            @PathVariable Long id,
            @RequestParam boolean underMaintenance) {
        return ResponseEntity.ok(assetService.setMaintenance(id, underMaintenance));
    }

    @PatchMapping("/{id}/dispose")
    public ResponseEntity<AssetResponse> dispose(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(assetService.dispose(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


