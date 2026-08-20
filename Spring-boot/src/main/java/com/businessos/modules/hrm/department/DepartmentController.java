package com.businessos.modules.hrm.department;

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
@RequestMapping("/api/departments")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@RequestBody DepartmentRequest request) {
        return new ResponseEntity<>(departmentService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<DepartmentResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(departmentService.listAll(PageRequest.of(page, size, Sort.by("name"))));
    }

    @GetMapping("/active")
    public ResponseEntity<List<DepartmentResponse>> listActive() {
        return ResponseEntity.ok(departmentService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> update(
            @PathVariable Long id,
            @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DepartmentResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


