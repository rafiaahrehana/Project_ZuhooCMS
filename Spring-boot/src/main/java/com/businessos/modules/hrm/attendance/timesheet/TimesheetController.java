package com.businessos.modules.hrm.attendance.timesheet;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/timesheets")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class TimesheetController {

    private final TimesheetService timesheetService;

    @PostMapping
    public ResponseEntity<TimesheetResponse> log(@Valid @RequestBody TimesheetRequest request) {
        return new ResponseEntity<>(timesheetService.log(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<TimesheetResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "31") int size) {
        return ResponseEntity.ok(timesheetService.listMine(
                PageRequest.of(page, size, Sort.by("workDate").descending())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<TimesheetResponse>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "31") int size) {
        return ResponseEntity.ok(timesheetService.listForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("workDate").descending())));
    }

    @GetMapping("/employee/{employeeId}/range")
    public ResponseEntity<List<TimesheetResponse>> listByRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(timesheetService.listByDateRange(employeeId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimesheetResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(timesheetService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TimesheetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetRequest request) {
        return ResponseEntity.ok(timesheetService.update(id, request));
    }

    @PostMapping("/ai-compose")
    public ResponseEntity<TimesheetComposeResponse> composeEntry(@Valid @RequestBody TimesheetComposeRequest request) {
        return ResponseEntity.ok(timesheetService.composeEntry(request));
    }

    @PostMapping("/submit")
    public ResponseEntity<java.util.Map<String, Integer>> submitForReview() {
        int count = timesheetService.submitForReview();
        return ResponseEntity.ok(java.util.Map.of("submitted", count));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<TimesheetResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(timesheetService.approve(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        timesheetService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


