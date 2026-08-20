package com.businessos.modules.hrm.recruitment.offerletter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/letters")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class OffertLetterController {

    private final OfferLetterService letterService;

    @PostMapping
    public ResponseEntity<OfferLetterResponse> create(@RequestBody OfferLetterRequest request) {
        return new ResponseEntity<>(letterService.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/draft")
    public ResponseEntity<OfferLetterDraftResponse> draftWithAi(@Valid @RequestBody OfferLetterDraftRequest request) {
        return ResponseEntity.ok(letterService.draftWithAi(request));
    }

    @GetMapping
    public ResponseEntity<Page<OfferLetterResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(letterService.listAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<OfferLetterResponse>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(letterService.listForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("issueDate").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferLetterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(letterService.getById(id));
    }

    @PatchMapping("/{id}/issue")
    public ResponseEntity<OfferLetterResponse> issue(@PathVariable Long id) {
        return ResponseEntity.ok(letterService.issue(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        letterService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


