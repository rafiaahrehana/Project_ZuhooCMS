package com.businessos.modules.crm.opportunity;

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
@RequiredArgsConstructor
@RequestMapping("/api/crm/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody OpportunityRequest request) {
        return new ResponseEntity<>(opportunityService.create(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/from-lead/{leadId}")
    public ResponseEntity<OpportunityResponse> createFromLead(
            @PathVariable Long leadId,
            @Valid @RequestBody OpportunityRequest request) {
        return new ResponseEntity<>(opportunityService.createFromLead(leadId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<OpportunityResponse>> listAll(
            @RequestParam(required = false) OpportunityStage stage,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(opportunityService.listAll(stage, clientId, ownerId, tagId, keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/pipeline-summary")
    public ResponseEntity<PipelineSummaryResponse> getPipelineSummary() {
        return ResponseEntity.ok(opportunityService.getPipelineSummary());
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<OpportunityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}")
    public ResponseEntity<OpportunityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/stage")
    public ResponseEntity<OpportunityResponse> changeStage(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStageRequest request) {
        return ResponseEntity.ok(opportunityService.changeStage(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}/won-duplicate-check")
    public ResponseEntity<com.businessos.modules.crm.duplicate.DuplicateMatch> previewWonDuplicate(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.previewWonDuplicate(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
