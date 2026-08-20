package com.businessos.modules.hrm.performance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/performance")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class PerformanceController {

    private final PerformanceService performanceService;

    @PostMapping
    public ResponseEntity<PerformanceReviewResponse> create(@RequestBody PerformanceReviewRequest request) {
        return new ResponseEntity<>(performanceService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<PerformanceReviewResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(performanceService.listAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<PerformanceReviewResponse>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(performanceService.listForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("reviewPeriodStart").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerformanceReviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.getById(id));
    }

    /**
     * Objective KPIs for an employee over a period. Computed live from
     * attendance, leave, tasks, service requests and client reviews.
     */
    @GetMapping("/employee/{employeeId}/kpis")
    public ResponseEntity<PerformanceKpiResponse> kpis(
            @PathVariable Long employeeId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(performanceService.kpisForEmployee(employeeId, from, to));
    }

    /** Signs off the current approval stage and moves to the next one. */
    @PostMapping("/{id}/advance")
    public ResponseEntity<PerformanceReviewResponse> advanceStage(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.advanceStage(id));
    }

    // ── Attachments ───────────────────────────────────────────────
    // The binary is uploaded first via POST /api/upload; these endpoints record
    // and manage the resulting URL against a review.

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<PerformanceAttachmentDtos.AttachmentResponse>> listAttachments(
            @PathVariable Long id) {
        return ResponseEntity.ok(performanceService.listAttachments(id));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<PerformanceAttachmentDtos.AttachmentResponse> addAttachment(
            @PathVariable Long id,
            @Valid @RequestBody PerformanceAttachmentDtos.AttachmentRequest request) {
        return new ResponseEntity<>(performanceService.addAttachment(id, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<String> deleteAttachment(
            @PathVariable Long id, @PathVariable Long attachmentId) {
        performanceService.deleteAttachment(id, attachmentId);
        return ResponseEntity.ok("Attachment removed");
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PerformanceReviewResponse> summarise(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.summarise(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PerformanceReviewResponse> update(
            @PathVariable Long id,
            @RequestBody PerformanceReviewRequest request) {
        return ResponseEntity.ok(performanceService.update(id, request));
    }

    @PatchMapping("/{id}/finalise")
    public ResponseEntity<PerformanceReviewResponse> finalise(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.finalise(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        performanceService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


