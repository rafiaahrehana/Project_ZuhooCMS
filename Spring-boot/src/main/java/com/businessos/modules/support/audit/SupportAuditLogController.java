package com.businessos.modules.support.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/support/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Support Audit Logs", description = "Support Audit Log Management")
public class SupportAuditLogController {

    private final SupportAuditService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get Audit Log by ID")
    public ResponseEntity<SupportAuditLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get all Audit Logs")
    public ResponseEntity<Page<SupportAuditLogResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/action/{actionType}")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get Audit Logs by Action Type")
    public ResponseEntity<Page<SupportAuditLogResponse>> getByActionType(
            @PathVariable String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByActionType(actionType, PageRequest.of(page, size)));
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get Audit Logs by Resource ID")
    public ResponseEntity<List<SupportAuditLogResponse>> getByResourceId(@PathVariable Long resourceId) {
        return ResponseEntity.ok(service.getByResourceId(resourceId));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get Audit Logs by Date Range")
    public ResponseEntity<Page<SupportAuditLogResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByDateRange(start, end, PageRequest.of(page, size)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER')")
    @Operation(summary = "Get Audit Logs by User ID")
    public ResponseEntity<Page<SupportAuditLogResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByUser(userId, PageRequest.of(page, size)));
    }
}
