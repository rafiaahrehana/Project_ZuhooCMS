package com.businessos.modules.support.sla;

import com.businessos.modules.support.ticket.TicketPriority;
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
@RequestMapping("/api/support/sla-policies")
@RequiredArgsConstructor
@Tag(name = "SLA Policies", description = "SLA Policy Management")
public class SLAPolicyController {

    private final SLAPolicyService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create SLA Policy")
    public ResponseEntity<SLAPolicyResponse> create(@Valid @RequestBody SLAPolicyRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get SLA Policy by ID")
    public ResponseEntity<SLAPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get SLA Policy by Priority")
    public ResponseEntity<SLAPolicyResponse> getByPriority(@PathVariable TicketPriority priority) {
        return ResponseEntity.ok(service.getByPriority(priority));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get all SLA Policies")
    public ResponseEntity<Page<SLAPolicyResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Active SLA Policies")
    public ResponseEntity<List<SLAPolicyResponse>> getActive() {
        return ResponseEntity.ok(service.getActive());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update SLA Policy")
    public ResponseEntity<SLAPolicyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SLAPolicyRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update SLA Policy Status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        service.updateStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Delete SLA Policy")
    public ResponseEntity<SLAPolicyResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
