package com.businessos.modules.hrm.recruitment.offer;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.ApplicationStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Offer lifecycle: DRAFT -> SENT -> ACCEPTED / DECLINED (or WITHDRAWN).
 * Sending the offer moves the application to OFFERED; the accepted offer's
 * salary breakdown is what onboarding pre-fills from. A SENT offer past its
 * expiry date reports expired=true - a derived fact, not a stored status, so
 * nothing has to run at midnight.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment/offers")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class JobOfferController {

    private final JobOfferRepository offerRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;
    private final EmailBranding emailBranding;

    // ── Read ──────────────────────────────────────────────────

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OfferResponse>> list(
            @RequestParam(required = false) JobOffer.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        Long companyId = requireCompanyId();
        Page<JobOffer> result = status != null
            ? offerRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status, PageRequest.of(page, size))
            : offerRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, PageRequest.of(page, size));
        return ResponseEntity.ok(result.map(OfferResponse::from));
    }

    @GetMapping("/application/{applicationId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OfferResponse>> forApplication(@PathVariable Long applicationId) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        requireApplication(applicationId);
        return ResponseEntity.ok(offerRepository.findByJobApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream().map(OfferResponse::from).toList());
    }

    // ── Create / edit (DRAFT only) ────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<OfferResponse> create(@RequestBody OfferRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Long companyId = requireCompanyId();
        JobApplication application = requireApplication(request.getJobApplicationId());
        if (application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application is closed - an offer can't be created for it");
        }
        // One live offer per application: a second offer while one is
        // pending/accepted is how companies double-commit by accident.
        if (offerRepository.existsByJobApplicationIdAndStatusIn(application.getId(),
                List.of(JobOffer.Status.DRAFT, JobOffer.Status.SENT, JobOffer.Status.ACCEPTED))) {
            throw new BadRequestException("This application already has an active offer - withdraw it first");
        }
        if (request.getOfferedJobTitle() == null || request.getOfferedJobTitle().isBlank()) {
            throw new BadRequestException("Offered job title is required");
        }
        // An offer with no expiry never expires, so it never forces a candidate
        // decision - it just sits SENT indefinitely.
        if (request.getExpiryDate() == null) {
            throw new BadRequestException("Expiry date is required");
        }
        if (request.getGrossSalary() == null) {
            throw new BadRequestException("Gross salary is required");
        }

        Company companyRef = new Company();
        companyRef.setId(companyId);

        JobOffer offer = JobOffer.builder()
                .company(companyRef)
                .jobApplication(application)
                .offeredJobTitle(request.getOfferedJobTitle().trim())
                .joiningDate(request.getJoiningDate())
                .expiryDate(request.getExpiryDate())
                .grossSalary(request.getGrossSalary())
                .basicSalary(request.getBasicSalary())
                .houseRent(request.getHouseRent())
                .medicalAllowance(request.getMedicalAllowance())
                .transportAllowance(request.getTransportAllowance())
                .notes(request.getNotes())
                .build();
        offerRepository.save(offer);
        application.setStatus(ApplicationStatus.OFFER_PENDING);
        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<OfferResponse> update(@PathVariable Long id, @RequestBody OfferRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.DRAFT) {
            throw new BadRequestException("Only a draft offer can be edited - the sent terms are the record");
        }
        if (request.getOfferedJobTitle() != null && !request.getOfferedJobTitle().isBlank()) {
            offer.setOfferedJobTitle(request.getOfferedJobTitle().trim());
        }
        offer.setJoiningDate(request.getJoiningDate());
        offer.setExpiryDate(request.getExpiryDate());
        offer.setGrossSalary(request.getGrossSalary());
        offer.setBasicSalary(request.getBasicSalary());
        offer.setHouseRent(request.getHouseRent());
        offer.setMedicalAllowance(request.getMedicalAllowance());
        offer.setTransportAllowance(request.getTransportAllowance());
        offer.setNotes(request.getNotes());
        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.DRAFT) {
            throw new BadRequestException("Only a draft offer can be deleted - withdraw a sent one instead");
        }
        // Otherwise the application is left showing OFFER_PENDING with zero
        // offers behind it - same "eligible for a fresh offer" landing spot
        // withdraw() uses.
        if (offer.getJobApplication().getStatus() == ApplicationStatus.OFFER_PENDING) {
            offer.getJobApplication().setStatus(ApplicationStatus.SELECTED);
        }
        offerRepository.delete(offer);
        return ResponseEntity.noContent().build();
    }

    // ── Lifecycle ─────────────────────────────────────────────

    @PatchMapping("/{id}/send")
    @Transactional
    public ResponseEntity<OfferResponse> send(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.DRAFT) {
            throw new BadRequestException("Only a draft offer can be sent");
        }
        offer.setStatus(JobOffer.Status.SENT);
        offer.setSentAt(LocalDateTime.now());
        JobApplication application = offer.getJobApplication();
        application.setStatus(ApplicationStatus.OFFER_SENT);

        // This is the only place an offer email actually goes out -
        // RecruitmentServiceImpl.updateStatus() no longer accepts any offer
        // sub-status at all, so there's exactly one path that can send it.
        try {
            Company fullCompany = companyRepository.findById(requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            EmailBranding.Data branding = emailBranding.from(fullCompany);
            emailService.sendOfferLetterEmail(application.getCandidate().getEmail(), application.getCandidate().getName(), branding);
        } catch (Exception ex) {
            log.warn("Offer letter email failed (offer still sent): {}", ex.getMessage());
        }

        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    @PatchMapping("/{id}/accept")
    @Transactional
    public ResponseEntity<OfferResponse> accept(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.SENT) {
            throw new BadRequestException("Only a sent offer can be accepted");
        }
        requireApplicationStatus(offer, ApplicationStatus.OFFER_SENT);
        offer.setStatus(JobOffer.Status.ACCEPTED);
        offer.setDecidedAt(LocalDateTime.now());
        offer.getJobApplication().setStatus(ApplicationStatus.OFFER_ACCEPTED);
        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    @PatchMapping("/{id}/decline")
    @Transactional
    public ResponseEntity<OfferResponse> decline(@PathVariable Long id, @RequestBody(required = false) DeclineRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.SENT) {
            throw new BadRequestException("Only a sent offer can be declined");
        }
        requireApplicationStatus(offer, ApplicationStatus.OFFER_SENT);
        offer.setStatus(JobOffer.Status.DECLINED);
        offer.setDecidedAt(LocalDateTime.now());
        offer.setDeclineReason(request != null ? request.getReason() : null);
        // A dedicated OFFER_REJECTED status now exists, so declining no longer
        // has to overload the application onto the generic REJECTED state.
        offer.getJobApplication().setStatus(ApplicationStatus.OFFER_REJECTED);
        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    @PatchMapping("/{id}/withdraw")
    @Transactional
    public ResponseEntity<OfferResponse> withdraw(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        JobOffer offer = requireOffer(id);
        if (offer.getStatus() != JobOffer.Status.SENT && offer.getStatus() != JobOffer.Status.DRAFT) {
            throw new BadRequestException("Only a draft or sent offer can be withdrawn");
        }
        requireApplicationStatus(offer, offer.getStatus() == JobOffer.Status.SENT
                ? ApplicationStatus.OFFER_SENT : ApplicationStatus.OFFER_PENDING);
        offer.setStatus(JobOffer.Status.WITHDRAWN);
        offer.setDecidedAt(LocalDateTime.now());
        // Withdrawing isn't the candidate's fault - leave them eligible for a
        // fresh offer instead of stuck on a dead-end offer sub-status.
        offer.getJobApplication().setStatus(ApplicationStatus.SELECTED);
        return ResponseEntity.ok(OfferResponse.from(offer));
    }

    // ── Helpers ───────────────────────────────────────────────

    // Defense-in-depth alongside RecruitmentServiceImpl.updateStatus()'s own
    // guard: if the application's status ever drifts away from what this
    // offer's own status implies (e.g. someone rerouted it through a path
    // that predates that guard), fail loudly here rather than silently
    // overwriting whatever the application currently shows.
    private void requireApplicationStatus(JobOffer offer, ApplicationStatus expected) {
        ApplicationStatus actual = offer.getJobApplication().getStatus();
        if (actual != expected) {
            throw new BadRequestException(
                    "This application's status (" + actual + ") no longer matches this offer - refresh and check its current state");
        }
    }

    private JobOffer requireOffer(Long id) {
        return offerRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + id));
    }

    private JobApplication requireApplication(Long id) {
        return applicationRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    // ── DTOs ──────────────────────────────────────────────────

    @Getter @Setter
    public static class OfferRequest {
        private Long jobApplicationId;
        private String offeredJobTitle;
        private LocalDate joiningDate;
        private LocalDate expiryDate;
        private BigDecimal grossSalary;
        private BigDecimal basicSalary;
        private BigDecimal houseRent;
        private BigDecimal medicalAllowance;
        private BigDecimal transportAllowance;
        private String notes;
    }

    @Getter @Setter
    public static class DeclineRequest {
        private String reason;
    }

    @Getter @Setter
    public static class OfferResponse {
        private Long id;
        private Long jobApplicationId;
        private String applicantName;
        private String applicantEmail;
        private String jobPostingTitle;
        private String applicationStatus;
        private String offeredJobTitle;
        private LocalDate joiningDate;
        private LocalDate expiryDate;
        private BigDecimal grossSalary;
        private BigDecimal basicSalary;
        private BigDecimal houseRent;
        private BigDecimal medicalAllowance;
        private BigDecimal transportAllowance;
        private JobOffer.Status status;
        private boolean expired;
        private LocalDateTime sentAt;
        private LocalDateTime decidedAt;
        private String declineReason;
        private String notes;
        private LocalDateTime createdAt;

        static OfferResponse from(JobOffer o) {
            OfferResponse r = new OfferResponse();
            r.id = o.getId();
            r.jobApplicationId = o.getJobApplication().getId();
            r.applicantName = o.getJobApplication().getCandidate() != null ? o.getJobApplication().getCandidate().getName() : null;
            r.applicantEmail = o.getJobApplication().getCandidate() != null ? o.getJobApplication().getCandidate().getEmail() : null;
            r.jobPostingTitle = o.getJobApplication().getJobPosting() != null
                    ? o.getJobApplication().getJobPosting().getTitle() : null;
            r.applicationStatus = o.getJobApplication().getStatus() != null
                    ? o.getJobApplication().getStatus().name() : null;
            r.offeredJobTitle = o.getOfferedJobTitle();
            r.joiningDate = o.getJoiningDate();
            r.expiryDate = o.getExpiryDate();
            r.grossSalary = o.getGrossSalary();
            r.basicSalary = o.getBasicSalary();
            r.houseRent = o.getHouseRent();
            r.medicalAllowance = o.getMedicalAllowance();
            r.transportAllowance = o.getTransportAllowance();
            r.status = o.getStatus();
            r.expired = o.getStatus() == JobOffer.Status.SENT
                    && o.getExpiryDate() != null && o.getExpiryDate().isBefore(LocalDate.now());
            r.sentAt = o.getSentAt();
            r.decidedAt = o.getDecidedAt();
            r.declineReason = o.getDeclineReason();
            r.notes = o.getNotes();
            r.createdAt = o.getCreatedAt();
            return r;
        }
    }
}
