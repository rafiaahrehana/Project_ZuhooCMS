package com.businessos.modules.hrm.salary;

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
@RequestMapping("/api/hr/salary-structures")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    @PostMapping
    public ResponseEntity<SalaryStructureResponse> create(@RequestBody SalaryStructureRequest request) {
        return new ResponseEntity<>(salaryStructureService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> update(
            @PathVariable Long id, @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(salaryStructureService.update(id, request));
    }

    /** Every structure in the company — the unfiltered list view. */
    @GetMapping
    public ResponseEntity<Page<SalaryStructureResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(salaryStructureService.listAll(
                PageRequest.of(page, size, Sort.by("effectiveFrom").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salaryStructureService.getById(id));
    }

    @GetMapping("/employee/{employeeId}/active")
    public ResponseEntity<SalaryStructureResponse> getActive(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryStructureService.getActiveForEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<SalaryStructureResponse>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(salaryStructureService.listForEmployee(employeeId,
                PageRequest.of(page, size, Sort.by("effectiveFrom").descending())));
    }

    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<List<SalaryStructureResponse>> history(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryStructureService.historyForEmployee(employeeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        salaryStructureService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


