package com.businessos.modules.hrm.recruitment.talentpool;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/** Talent pool: qualified candidates kept warm for future openings. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment/talent-pool")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class TalentPoolController {

    private final TalentPoolRepository poolRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<CandidateResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        Long companyId = requireCompanyId();
        Page<TalentPoolCandidate> result = (keyword != null && !keyword.isBlank())
            ? poolRepository.search(companyId, keyword.trim(), PageRequest.of(page, size))
            : poolRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageRequest.of(page, size));
        return ResponseEntity.ok(result.map(CandidateResponse::from));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CandidateResponse> create(@RequestBody CandidateRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Long companyId = requireCompanyId();
        validate(request);
        if (poolRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, request.getEmail().trim())) {
            throw new BadRequestException("A pooled candidate with that email already exists");
        }
        TalentPoolCandidate candidate = new TalentPoolCandidate();
        Company companyRef = new Company();
        companyRef.setId(companyId);
        candidate.setCompany(companyRef);
        apply(candidate, request);
        return ResponseEntity.ok(CandidateResponse.from(poolRepository.save(candidate)));
    }

    /** One-click pooling of a closed application - contact details come across. */
    @PostMapping("/from-application/{applicationId}")
    @Transactional
    public ResponseEntity<CandidateResponse> fromApplication(@PathVariable Long applicationId,
                                                             @RequestBody(required = false) PoolRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Long companyId = requireCompanyId();
        JobApplication application = applicationRepository.findByIdAndCompanyId(applicationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        // Claimed to check this but didn't - an active mid-pipeline candidate
        // could be pooled and tracked in two places at once. OFFER_REJECTED is
        // included alongside REJECTED/WITHDRAWN - a candidate who declined an
        // offer is exactly who this feature is for, and since the offer
        // sub-pipeline split off from the generic REJECTED status, this check
        // has to name it explicitly or that path is silently unreachable.
        if (application.getStatus() != com.businessos.enums.ApplicationStatus.REJECTED
                && application.getStatus() != com.businessos.enums.ApplicationStatus.WITHDRAWN
                && application.getStatus() != com.businessos.enums.ApplicationStatus.OFFER_REJECTED) {
            throw new BadRequestException(
                    "Only a rejected, withdrawn, or declined-offer application can be added to the talent pool");
        }
        com.businessos.modules.hrm.recruitment.candidate.Candidate person = application.getCandidate();
        if (poolRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, person.getEmail())) {
            throw new BadRequestException("This candidate is already in the talent pool");
        }

        Company companyRef = new Company();
        companyRef.setId(companyId);

        TalentPoolCandidate candidate = TalentPoolCandidate.builder()
                .company(companyRef)
                .name(person.getName())
                .email(person.getEmail())
                .phone(person.getPhone())
                .resumeUrl(person.getResumeUrl())
                .linkedInUrl(person.getLinkedInUrl())
                .desiredRole(application.getJobPosting() != null ? application.getJobPosting().getTitle() : null)
                .reason(request != null && request.getReason() != null ? request.getReason() : TalentPoolCandidate.Reason.FUTURE_FIT)
                .notes(request != null ? request.getNotes() : null)
                .sourceApplication(application)
                .build();
        return ResponseEntity.ok(CandidateResponse.from(poolRepository.save(candidate)));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<CandidateResponse> update(@PathVariable Long id, @RequestBody CandidateRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        TalentPoolCandidate candidate = requireCandidate(id);
        validate(request);
        apply(candidate, request);
        return ResponseEntity.ok(CandidateResponse.from(candidate));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        poolRepository.delete(requireCandidate(id));
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void validate(CandidateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private void apply(TalentPoolCandidate candidate, CandidateRequest request) {
        candidate.setName(request.getName().trim());
        candidate.setEmail(request.getEmail().trim());
        candidate.setPhone(request.getPhone());
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setLinkedInUrl(request.getLinkedInUrl());
        candidate.setDesiredRole(request.getDesiredRole());
        candidate.setSkills(request.getSkills());
        candidate.setRating(request.getRating());
        if (request.getReason() != null) candidate.setReason(request.getReason());
        candidate.setNotes(request.getNotes());
    }

    private TalentPoolCandidate requireCandidate(Long id) {
        return poolRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    // ── DTOs ──────────────────────────────────────────────────

    @Getter @Setter
    public static class CandidateRequest {
        private String name;
        private String email;
        private String phone;
        private String resumeUrl;
        private String linkedInUrl;
        private String desiredRole;
        private String skills;
        private Integer rating;
        private TalentPoolCandidate.Reason reason;
        private String notes;
    }

    @Getter @Setter
    public static class PoolRequest {
        private TalentPoolCandidate.Reason reason;
        private String notes;
    }

    @Getter @Setter
    public static class CandidateResponse {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String resumeUrl;
        private String linkedInUrl;
        private String desiredRole;
        private String skills;
        private Integer rating;
        private TalentPoolCandidate.Reason reason;
        private String notes;
        private Long sourceApplicationId;
        private String sourceJobTitle;
        private LocalDateTime createdAt;

        static CandidateResponse from(TalentPoolCandidate c) {
            CandidateResponse r = new CandidateResponse();
            r.id = c.getId();
            r.name = c.getName();
            r.email = c.getEmail();
            r.phone = c.getPhone();
            r.resumeUrl = c.getResumeUrl();
            r.linkedInUrl = c.getLinkedInUrl();
            r.desiredRole = c.getDesiredRole();
            r.skills = c.getSkills();
            r.rating = c.getRating();
            r.reason = c.getReason();
            r.notes = c.getNotes();
            if (c.getSourceApplication() != null) {
                r.sourceApplicationId = c.getSourceApplication().getId();
                r.sourceJobTitle = c.getSourceApplication().getJobPosting() != null
                        ? c.getSourceApplication().getJobPosting().getTitle() : null;
            }
            r.createdAt = c.getCreatedAt();
            return r;
        }
    }
}
