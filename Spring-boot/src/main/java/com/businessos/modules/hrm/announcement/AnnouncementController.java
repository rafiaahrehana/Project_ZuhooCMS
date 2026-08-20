package com.businessos.modules.hrm.announcement;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announcements")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return new ResponseEntity<>(announcementService.create(request), HttpStatus.CREATED);
    }

    /** Ask AI to draft a title/body for review - not persisted until the user saves it via POST above. */
    @PostMapping("/ai-draft")
    public ResponseEntity<AnnouncementDraftResponse> draftWithAi(@Valid @RequestBody AnnouncementDraftRequest request) {
        return ResponseEntity.ok(announcementService.draftWithAi(request));
    }

    @GetMapping
    public ResponseEntity<Page<AnnouncementResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(announcementService.listAll(
                (Pageable) PageRequest.of(page, size, Sort.by("priority").descending().and(Sort.by("createdAt").descending()))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AnnouncementResponse>> listActive() {
        return ResponseEntity.ok(announcementService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.update(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<AnnouncementResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.publish(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


