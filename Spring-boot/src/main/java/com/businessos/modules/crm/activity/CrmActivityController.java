package com.businessos.modules.crm.activity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/crm/activities")
public class CrmActivityController {

    private final CrmActivityService crmActivityService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<CrmActivityResponse> log(@Valid @RequestBody CrmActivityRequest request) {
        return new ResponseEntity<>(crmActivityService.log(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<CrmActivityResponse>> getTimeline(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(crmActivityService.getTimeline(clientId, opportunityId,
                PageRequest.of(page, size)));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/summary")
    public ResponseEntity<CrmActivitySummaryResponse> summarise(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long opportunityId) {
        return ResponseEntity.ok(crmActivityService.summarise(clientId, opportunityId));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<CrmActivityResponse> markCompleted(@PathVariable Long id) {
        return ResponseEntity.ok(crmActivityService.markCompleted(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        crmActivityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
