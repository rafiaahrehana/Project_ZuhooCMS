package com.businessos.modules.hrm.employee;

import com.businessos.enums.EmploymentStatus;
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
@RequestMapping("/api/employees")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeePdfService employeePdfService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@RequestBody CreateEmployeeRequest request) {
        return new ResponseEntity<>(employeeService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> listAll(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean excludeOwner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(employeeService.listAll(departmentId, status, search, excludeOwner,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean excludeOwner) {
        byte[] pdf = employeePdfService.generateListPdf(departmentId, status, search, excludeOwner);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile() {
        return ResponseEntity.ok(employeeService.getMyProfile());
    }

    @PatchMapping("/me")
    public ResponseEntity<EmployeeResponse> updateMyProfile(@RequestBody SelfUpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateMyProfile(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> terminate(@PathVariable Long id) {
        employeeService.terminate(id);
        return ResponseEntity.ok("Employee terminated successfully");
    }
}


