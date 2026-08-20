package com.businessos.modules.itam.offboarding;

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
@RequestMapping("/api/v1/company/offboarding/checklist")
@RequiredArgsConstructor
public class OffboardingController {

    private final OffboardingChecklistService checklistService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<OffboardingChecklistResponse> createChecklist(
            @Valid @RequestBody OffboardingChecklistRequest request) {
        return new ResponseEntity<>(checklistService.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<OffboardingChecklistResponse> getChecklistById(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<OffboardingChecklistResponse> getChecklistByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(checklistService.getByEmployee(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Page<OffboardingChecklistResponse>> getAllChecklists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(checklistService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<List<OffboardingChecklistResponse>> getPendingChecklists() {
        return ResponseEntity.ok(checklistService.getPendingChecklists());
    }

    @PatchMapping("/{id}/hardware-collected")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> markHardwareCollected(@PathVariable Long id,
            @RequestBody(required = false) OffboardingStepUpdateRequest body) {
        checklistService.markHardwareCollected(id, body != null ? body.getNotes() : null);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/licenses-revoked")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> markLicensesRevoked(@PathVariable Long id,
            @RequestBody(required = false) OffboardingStepUpdateRequest body) {
        checklistService.markLicensesRevoked(id, body != null ? body.getNotes() : null);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/access-revoked")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> markAccessRevoked(@PathVariable Long id,
            @RequestBody(required = false) OffboardingStepUpdateRequest body) {
        checklistService.markAccessRevoked(id, body != null ? body.getNotes() : null);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/data-handed-over")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> markDataHandedOver(@PathVariable Long id,
            @RequestBody(required = false) OffboardingStepUpdateRequest body) {
        checklistService.markDataHandedOver(id, body != null ? body.getNotes() : null);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/exit-interview")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<Void> markExitInterviewCompleted(@PathVariable Long id,
            @RequestBody(required = false) OffboardingStepUpdateRequest body) {
        checklistService.markExitInterviewCompleted(id, body != null ? body.getNotes() : null);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<OffboardingChecklistResponse> deleteChecklist(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.delete(id));
    }
}
