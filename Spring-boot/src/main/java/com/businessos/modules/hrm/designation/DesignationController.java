package com.businessos.modules.hrm.designation;

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
@RequestMapping("/api/hr/designations")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    public ResponseEntity<DesignationResponse> create(@RequestBody DesignationRequest request) {
        return new ResponseEntity<>(designationService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<DesignationResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(designationService.listAll(PageRequest.of(page, size, Sort.by("level"))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<DesignationResponse>> listActive() {
        return ResponseEntity.ok(designationService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignationResponse> update(
            @PathVariable Long id,
            @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(designationService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DesignationResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


