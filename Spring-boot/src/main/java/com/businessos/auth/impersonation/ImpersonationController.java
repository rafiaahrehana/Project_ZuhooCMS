package com.businessos.auth.impersonation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/platform-admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER')")
public class ImpersonationController {

    private final ImpersonationService impersonationService;
    private final ImpersonationAuditLogRepository impersonationAuditLogRepository;

    @PostMapping("/companies/{companyId}/impersonate")
    public ResponseEntity<ImpersonationResponse> impersonate(
            @PathVariable Long companyId,
            @Valid @RequestBody ImpersonateRequest request) {
        return ResponseEntity.ok(impersonationService.startImpersonation(companyId, request));
    }

    @PostMapping("/impersonate/end")
    public ResponseEntity<Void> endImpersonation(@Valid @RequestBody EndImpersonationRequest request) {
        impersonationService.endImpersonation(request);
        return ResponseEntity.ok().build();
    }

    // Was write-only - no endpoint anywhere read these back. Restricted to
    // admins/managers (not plain SUPPORT_AGENT), matching
    // SupportContextSwitchController.getContextSwitchHistory()'s equivalent
    // "review who did what" restriction for the same kind of compliance record.
    @GetMapping("/impersonate/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<Page<ImpersonationAuditLogResponse>> history(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<ImpersonationAuditLog> logs = companyId != null
                ? impersonationAuditLogRepository.findByCompanyIdOrderByStartedAtDesc(companyId, pageable)
                : impersonationAuditLogRepository.findAllByOrderByStartedAtDesc(pageable);
        return ResponseEntity.ok(logs.map(ImpersonationAuditLogResponse::from));
    }
}
