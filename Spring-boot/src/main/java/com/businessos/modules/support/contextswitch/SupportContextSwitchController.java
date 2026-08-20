package com.businessos.modules.support.contextswitch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/support/context-switches")
@RequiredArgsConstructor
@Tag(name = "Support Context Switches", description = "Support Agent Context Switch Management")
public class SupportContextSwitchController {

    private final SupportContextSwitchService service;

    @PostMapping("/switch")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT')")
    @Operation(summary = "Switch Context to a Company")
    public ResponseEntity<SupportContextSwitchResponse> switchContext(
            @Valid @RequestBody SupportContextSwitchRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(service.switchContext(request, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    // Same X-Forwarded-For-first pattern as AuthServiceImpl.resolveClientIp() -
    // the client's raw remote address is the proxy's address once behind one.
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT')")
    @Operation(summary = "End Context Switch")
    public ResponseEntity<Void> endContextSwitch(@PathVariable Long id) {
        service.endContextSwitch(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active/agent/{supportAgentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER') or hasRole('SUPPORT_AGENT')")
    @Operation(summary = "Get Active Context Switch for an Agent")
    public ResponseEntity<SupportContextSwitchResponse> getActiveContextSwitch(@PathVariable Long supportAgentId) {
        return ResponseEntity.ok(service.getActiveContextSwitch(supportAgentId));
    }

    @GetMapping("/history/agent/{supportAgentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Get Context Switch History for an Agent")
    public ResponseEntity<Page<SupportContextSwitchResponse>> getContextSwitchHistory(
            @PathVariable Long supportAgentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getContextSwitchHistory(supportAgentId, PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Get All Active Context Switches")
    public ResponseEntity<List<SupportContextSwitchResponse>> getActiveContextSwitches() {
        return ResponseEntity.ok(service.getActiveContextSwitches());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SUPPORT_MANAGER')")
    @Operation(summary = "Get Context Switch by ID")
    public ResponseEntity<SupportContextSwitchResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
