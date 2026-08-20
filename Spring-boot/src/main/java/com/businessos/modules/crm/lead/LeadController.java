package com.businessos.modules.crm.lead;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/crm/leads")
public class LeadController {

    private final LeadService leadService;
    private final LeadPdfService leadPdfService;
    private final LeadCsvImportService leadCsvImportService;

    /**
     * Bulk import from a CSV file. Permission is checked in the service;
     * the response reports what was created and every skipped line with why.
     */
    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @org.springframework.web.bind.annotation.PostMapping("/import")
    public org.springframework.http.ResponseEntity<LeadCsvImportService.ImportResult> importCsv(
            @org.springframework.web.bind.annotation.RequestParam("file")
            org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        return org.springframework.http.ResponseEntity.ok(
                leadCsvImportService.importCsv(file.getInputStream()));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest request) {
        return new ResponseEntity<>(leadService.createLead(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}")
    public ResponseEntity<LeadResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<Page<LeadResponse>> listAll(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(leadService.listLeads(status, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam(required = false) LeadStatus status) {
        byte[] pdf = leadPdfService.generateListPdf(status);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/my")
    public ResponseEntity<Page<LeadResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leadService.listMyLeads(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/search")
    public ResponseEntity<Page<LeadResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.searchLeads(keyword, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/filter")
    public ResponseEntity<Page<LeadResponse>> filter(
            @Valid @RequestBody LeadFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.filterLeads(filterRequest, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/by-source/{source}")
    public ResponseEntity<Page<LeadResponse>> getBySource(
            @PathVariable LeadSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.findLeadsBySource(source, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/by-priority/{priority}")
    public ResponseEntity<Page<LeadResponse>> getByPriority(
            @PathVariable Priority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.findLeadsByPriority(priority, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/unassigned")
    public ResponseEntity<Page<LeadResponse>> getUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.findUnassignedLeads(null, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/high-priority")
    public ResponseEntity<Page<LeadResponse>> getHighPriority(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("expectedCloseDate").ascending());
        return ResponseEntity.ok(leadService.findHighPriorityOpenLeads(null, pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/never-contacted")
    public ResponseEntity<Page<LeadResponse>> getNeverContacted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(leadService.findNeverContactedLeads(pageable));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/stale")
    public ResponseEntity<Page<LeadResponse>> getStale(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivityAt").ascending());
        return ResponseEntity.ok(leadService.findStalLeads(pageable));
    }

    // ==================== Lead Conversion ====================

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PatchMapping("/{id}/convert-to-opportunity")
    public ResponseEntity<com.businessos.modules.crm.opportunity.OpportunityResponse> convertToOpportunity(
            @PathVariable Long id, @Valid @RequestBody ConvertToOpportunityRequest request) {
        return ResponseEntity.ok(leadService.convertToOpportunity(id, request));
    }

    // ==================== Activity Timeline ====================

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @PostMapping("/{leadId}/activities")
    public ResponseEntity<com.businessos.modules.crm.activity.CrmActivityResponse> addActivity(
            @PathVariable Long leadId,
            @Valid @RequestBody com.businessos.modules.crm.activity.CrmActivityRequest request) {
        return new ResponseEntity<>(leadService.addActivity(leadId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{leadId}/activities")
    public ResponseEntity<Page<com.businessos.modules.crm.activity.CrmActivityResponse>> listActivities(
            @PathVariable Long leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leadService.getActivities(leadId,
                PageRequest.of(page, size, Sort.by("activityDate").descending())));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @DeleteMapping("/{leadId}/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long leadId,
            @PathVariable Long activityId) {
        leadService.deleteActivity(leadId, activityId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Dashboard Endpoints ====================

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/stats/by-status/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable LeadStatus status) {
        return ResponseEntity.ok(leadService.countLeadsByStatus(status));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/stats/active")
    public ResponseEntity<Long> countActive() {
        return ResponseEntity.ok(leadService.countActiveLeads());
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/stats/my-active")
    public ResponseEntity<Long> countMyActive() {
        return ResponseEntity.ok(leadService.countMyActiveLeads());
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/{id}/summary")
    public ResponseEntity<LeadResponse> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.summariseLead(id));
    }
}
