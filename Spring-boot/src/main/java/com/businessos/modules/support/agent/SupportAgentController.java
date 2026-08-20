package com.businessos.modules.support.agent;

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
@RequiredArgsConstructor
@RequestMapping("/api/v1/support/agents")
public class SupportAgentController {

    private final SupportAgentService agentService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<SupportAgentResponse> create(@Valid @RequestBody SupportAgentRequest request) {
        return new ResponseEntity<>(agentService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<Page<SupportAgentResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<SupportAgentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_AGENT') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<SupportAgentResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(agentService.getByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<Page<SupportAgentResponse>> getByStatus(
            @PathVariable SupportAgentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentService.getByStatus(status, PageRequest.of(page, size)));
    }

    // Also needed by whoever can assign a ticket to an agent (see
    // SupportTicketController#assign) - COMPANY_OWNER/EMPLOYEE need this to populate
    // the assignee picker, not just to manage agent records.
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_MANAGER', 'SUPPORT_AGENT', 'COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<SupportAgentResponse>> getAvailable() {
        return ResponseEntity.ok(agentService.getAvailableAgents());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<SupportAgentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportAgentRequest request) {
        return ResponseEntity.ok(agentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam SupportAgentStatus status) {
        agentService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/accepting-tickets")
    @PreAuthorize("hasRole('SUPPORT_AGENT') OR hasRole('SUPPORT_MANAGER')")
    public ResponseEntity<Void> updateAcceptingTickets(
            @PathVariable Long id,
            @RequestParam boolean accepting) {
        agentService.updateAcceptingTickets(id, accepting);
        return ResponseEntity.ok().build();
    }
}