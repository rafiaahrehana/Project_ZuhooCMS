package com.businessos.modules.hrm.recruitment.jobpost;

import com.businessos.enums.JobPostingStatus;
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
@RequestMapping("/api/recruitment/jobs")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(@RequestBody JobPostingRequest request) {
        return new ResponseEntity<>(jobPostingService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<JobPostingResponse>> listAll(
            @RequestParam(required = false) JobPostingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobPostingService.listAll(status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/open")
    public ResponseEntity<List<JobPostingResponse>> listOpen() {
        return ResponseEntity.ok(jobPostingService.listOpen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable Long id,
            @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(jobPostingService.update(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<JobPostingResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.publish(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<JobPostingResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.close(id));
    }

    @PatchMapping("/{id}/assign-recruiter")
    public ResponseEntity<JobPostingResponse> assignRecruiter(
            @PathVariable Long id, @RequestBody AssignRecruiterRequest request) {
        return ResponseEntity.ok(jobPostingService.assignRecruiter(id, request.getRecruiterId()));
    }

    @lombok.Getter @lombok.Setter
    public static class AssignRecruiterRequest {
        private Long recruiterId;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}


