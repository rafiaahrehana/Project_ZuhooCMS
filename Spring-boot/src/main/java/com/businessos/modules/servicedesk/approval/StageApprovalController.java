package com.businessos.modules.servicedesk.approval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@Tag(name = "Stage Approvals", description = "Service Request Workflow Stage Approval Management")
public class StageApprovalController {

    private final StageApprovalService service;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get Pending Stage Approvals")
    public ResponseEntity<Page<StageApprovalResponse>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getPending(PageRequest.of(page, size)));
    }

    @GetMapping("/request/{serviceRequestId}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'CLIENT')")
    @Operation(summary = "Get Stage Approvals for a Service Request")
    public ResponseEntity<List<StageApprovalResponse>> getForRequest(@PathVariable Long serviceRequestId) {
        return ResponseEntity.ok(service.getForRequest(serviceRequestId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Approve Stage Approval Request")
    public ResponseEntity<StageApprovalResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Reject Stage Approval Request")
    public ResponseEntity<StageApprovalResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }
}
