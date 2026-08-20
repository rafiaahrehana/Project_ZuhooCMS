package com.businessos.modules.hrm.recruitment;

import com.businessos.modules.hrm.employee.EmployeeResponse;
import com.businessos.modules.hrm.recruitment.jobapplication.EvaluateCandidateRequest;
import com.businessos.modules.hrm.recruitment.jobapplication.HireApplicationRequest;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRequest;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationResponse;
import com.businessos.modules.hrm.recruitment.jobapplication.UpdateApplicationStatusRequest;
import com.businessos.enums.ApplicationStatus;
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
@RequestMapping("/api/recruitment")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @PostMapping("/jobs/{jobPostingId}/apply")
    public ResponseEntity<JobApplicationResponse> apply(
            @PathVariable Long jobPostingId,
            @RequestBody JobApplicationRequest request) {
        return new ResponseEntity<>(recruitmentService.apply(jobPostingId, request), HttpStatus.CREATED);
    }

    @GetMapping("/applications")
    public ResponseEntity<Page<JobApplicationResponse>> listAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recruitmentService.listAll(status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/jobs/{jobPostingId}/applications")
    public ResponseEntity<Page<JobApplicationResponse>> listByPosting(
            @PathVariable Long jobPostingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recruitmentService.listByPosting(jobPostingId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<JobApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getById(id));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateApplicationStatusRequest request) {
        return ResponseEntity.ok(recruitmentService.updateStatus(id, request.getStatus(), request.getNotes()));
    }

    @PatchMapping("/applications/{id}/evaluate")
    public ResponseEntity<JobApplicationResponse> evaluate(
            @PathVariable Long id,
            @RequestBody EvaluateCandidateRequest request) {
        return ResponseEntity.ok(recruitmentService.evaluate(id, request));
    }

    @PostMapping("/applications/{id}/hire")
    public ResponseEntity<EmployeeResponse> hire(
            @PathVariable Long id,
            @RequestBody HireApplicationRequest request) {
        return new ResponseEntity<>(recruitmentService.hire(id, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        recruitmentService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


