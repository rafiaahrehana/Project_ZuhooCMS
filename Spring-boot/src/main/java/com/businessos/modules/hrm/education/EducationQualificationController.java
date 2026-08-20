package com.businessos.modules.hrm.education;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/education-qualifications")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class EducationQualificationController {

    private final EducationQualificationService qualificationService;

    @PostMapping
    public ResponseEntity<EducationQualificationResponse> create(@Valid @RequestBody EducationQualificationRequest request) {
        return new ResponseEntity<>(qualificationService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EducationQualificationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EducationQualificationRequest request) {
        return ResponseEntity.ok(qualificationService.update(id, request));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EducationQualificationResponse>> listForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(qualificationService.listForEmployee(employeeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        qualificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
