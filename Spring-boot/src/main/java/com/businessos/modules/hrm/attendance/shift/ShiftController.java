package com.businessos.modules.hrm.attendance.shift;

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
@RequestMapping("/api/hr/shifts")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<ShiftResponse> create(@Valid @RequestBody ShiftRequest request) {
        return new ResponseEntity<>(shiftService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ShiftResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shiftService.listAll(PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ShiftResponse>> listActive() {
        return ResponseEntity.ok(shiftService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(shiftService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ShiftResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        shiftService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


