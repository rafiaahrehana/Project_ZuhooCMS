package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationResponse;
import com.businessos.modules.hrm.recruitment.RecruitmentMapper;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Candidates: the person, independent of any one job application - see JobApplication for the per-job pipeline state. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment/candidates")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class CandidateController {

    private final CandidateService candidateService;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<Page<CandidateResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(candidateService.list(q,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }

    @GetMapping("/{id}/applications")
    @Transactional(readOnly = true)
    public ResponseEntity<List<JobApplicationResponse>> applications(@PathVariable Long id) {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) throw new BadRequestException("No company context");
        return ResponseEntity.ok(applicationRepository.findByCompanyIdAndCandidateId(companyId, id).stream()
                .map(RecruitmentMapper::toJobApplicationResponse).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CandidateResponse> update(@PathVariable Long id, @RequestBody CandidateRequest request) {
        return ResponseEntity.ok(candidateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
