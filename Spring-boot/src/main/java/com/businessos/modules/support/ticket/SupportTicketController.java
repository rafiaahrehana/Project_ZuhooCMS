package com.businessos.modules.support.ticket;

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

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets", description = "Support Ticket Management")
public class SupportTicketController {

    private final SupportTicketService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Create Support Ticket")
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody SupportTicketRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Ticket by ID")
    public ResponseEntity<SupportTicketResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/number/{number}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE', 'SUPPORT_AGENT', 'SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Ticket by Number")
    public ResponseEntity<SupportTicketResponse> getByNumber(@PathVariable String number) {
        return ResponseEntity.ok(service.getByTicketNumber(number));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get all Tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/my-tickets")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Get My Tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getMyTickets(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getMyTickets(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    @Operation(summary = "Get Tickets Assigned to Me")
    public ResponseEntity<Page<SupportTicketResponse>> getAssignedToMe(
            @RequestParam(required = false) Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getAssignedToMe(agentId, PageRequest.of(page, size)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Tickets by Status")
    public ResponseEntity<Page<SupportTicketResponse>> getByStatus(
            @PathVariable TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/sla-breached")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get SLA Breached Tickets")
    public ResponseEntity<?> getSLABreached() {
        return ResponseEntity.ok(service.getSLABreachedTickets());
    }

    @GetMapping("/critical-open")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get Critical Open Tickets")
    public ResponseEntity<?> getCriticalOpen() {
        return ResponseEntity.ok(service.getOpenCriticalTickets());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Assign Ticket to Agent")
    public ResponseEntity<Void> assign(
            @PathVariable Long id,
            @RequestParam Long agentId) {
        service.assignToAgent(id, agentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Reassign Ticket")
    public ResponseEntity<Void> reassign(
            @PathVariable Long id,
            @RequestParam Long newAgentId,
            @RequestParam String reason) {
        service.reassignToAgent(id, newAgentId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Escalate Ticket")
    public ResponseEntity<Void> escalate(
            @PathVariable Long id,
            @RequestParam String reason) {
        service.escalate(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/first-response")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Record First Response (SLA Timer)")
    public ResponseEntity<Void> recordFirstResponse(@PathVariable Long id) {
        service.recordFirstResponse(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Resolve Ticket")
    public ResponseEntity<Void> resolve(
            @PathVariable Long id,
            @RequestParam String notes) {
        service.resolve(id, notes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('SUPPORT_MANAGER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Close Ticket")
    public ResponseEntity<Void> close(@PathVariable Long id) {
        service.close(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'SUPPORT_MANAGER', 'COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Reopen Ticket")
    public ResponseEntity<Void> reopen(
            @PathVariable Long id,
            @RequestParam String reason) {
        service.reopen(id, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/satisfaction")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "Record Customer Satisfaction")
    public ResponseEntity<Void> recordSatisfaction(
            @PathVariable Long id,
            @RequestParam int rating,
            @RequestParam String feedback) {
        service.recordSatisfaction(id, rating, feedback);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT') or hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update Ticket")
    public ResponseEntity<SupportTicketResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Delete Ticket")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── CLIENT-facing (CUSTOMER_SUPPORT tickets) ────────────────────
    // Distinct from create()/getMyTickets() above, which are PLATFORM_SUPPORT
    // only (tenant staff -> BusinessOS). These raise/list/view a ticket against
    // the client's own client-company instead, ownership-checked in the service.

    @PostMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "[Client] Raise a Support Ticket")
    public ResponseEntity<SupportTicketResponse> createForClient(@Valid @RequestBody SupportTicketRequest request) {
        return new ResponseEntity<>(service.createForClient(request), HttpStatus.CREATED);
    }

    @GetMapping("/client/my")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "[Client] List My Tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getMyClientTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getMyClientTickets(PageRequest.of(page, size)));
    }

    @GetMapping("/client/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "[Client] Get One of My Tickets")
    public ResponseEntity<SupportTicketResponse> getClientTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getClientTicketById(id));
    }

    // ── STAFF-facing "Client Chat" (CUSTOMER_SUPPORT tickets) ───────
    // This company's own clients' tickets - the counterpart to getAll()/
    // getByStatus() above, which only ever return PLATFORM_SUPPORT tickets.

    @GetMapping("/company/client-tickets")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "[Staff] List This Company's Client Tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getClientTicketsForCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getClientTicketsForCompany(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/company/client-tickets/status/{status}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @Operation(summary = "[Staff] List This Company's Client Tickets by Status")
    public ResponseEntity<Page<SupportTicketResponse>> getClientTicketsForCompanyByStatus(
            @PathVariable TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getClientTicketsForCompanyByStatus(status, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}

