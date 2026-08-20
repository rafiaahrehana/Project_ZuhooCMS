package com.businessos.modules.hrm.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/asset-history")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class AssetAssignmentHistoryController {

    private final AssetAssignmentHistoryService historyService;

    @GetMapping
    public ResponseEntity<Page<AssetAssignmentHistoryResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.listAll(
                PageRequest.of(page, size, Sort.by("assignedAt").descending())));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<Page<AssetAssignmentHistoryResponse>> byAsset(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.historyForAsset(assetId,
                PageRequest.of(page, size, Sort.by("assignedAt").descending())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<AssetAssignmentHistoryResponse>> byEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(historyService.historyForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("assignedAt").descending())));
    }
}


