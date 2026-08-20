package com.businessos.modules.hrm.recruitment.careerpage;

import com.businessos.enums.ApplicationSource;
import com.businessos.enums.ApplicationStatus;
import com.businessos.enums.JobPostingStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.recruitment.ats.CvScoringService;
import com.businessos.modules.hrm.recruitment.candidate.Candidate;
import com.businessos.modules.hrm.recruitment.candidate.CandidateService;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;
import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.storage.LocalFileStorageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The PUBLIC careers page - no authentication. The slug is the only tenant
 * key: every query is scoped through the CareerPageSettings row it resolves
 * to, and only OPEN postings whose deadline hasn't passed are ever exposed.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/careers")
public class PublicCareersController {

    private final CareerPageSettingsRepository settingsRepository;
    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository applicationRepository;
    private final CandidateService candidateService;
    private final CvScoringService cvScoringService;
    private final LocalFileStorageService fileStorageService;

    @GetMapping("/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<CareerPageView> page(@PathVariable String slug) {
        CareerPageSettings settings = requirePublished(slug);
        Company company = companyRepository.findById(settings.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Career page not found"));

        CareerPageView view = new CareerPageView();
        view.companyName = company.getCompanyName();
        view.headline = settings.getHeadline();
        view.about = settings.getAbout();
        view.brandColor = settings.getBrandColor();
        view.jobs = openPostings(settings.getCompanyId()).stream().map(JobCard::from).toList();
        return ResponseEntity.ok(view);
    }

    @GetMapping("/{slug}/jobs/{jobId}")
    @Transactional(readOnly = true)
    public ResponseEntity<JobDetail> job(@PathVariable String slug, @PathVariable Long jobId) {
        CareerPageSettings settings = requirePublished(slug);
        return ResponseEntity.ok(JobDetail.from(requireOpenPosting(settings, jobId)));
    }

    @PostMapping("/{slug}/jobs/{jobId}/apply")
    @Transactional
    public ResponseEntity<ApplyResult> apply(@PathVariable String slug, @PathVariable Long jobId,
                                             @RequestBody ApplyRequest request) {
        CareerPageSettings settings = requirePublished(slug);
        JobPosting posting = requireOpenPosting(settings, jobId);

        // Honeypot: real users never see this field; bots that fill every
        // input get a success response and no record.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            return ResponseEntity.ok(new ApplyResult("Application received"));
        }

        String name = trimToNull(request.getApplicantName());
        // Lowercase to match RecruitmentServiceImpl.apply()'s duplicate check -
        // without it, "Jane@Gmail.com" and "jane@gmail.com" bypassed the
        // duplicate guard entirely on the actual public entry point.
        String email = trimToNull(request.getApplicantEmail() != null ? request.getApplicantEmail().toLowerCase() : null);
        if (name == null || email == null) {
            throw new BadRequestException("Name and email are required");
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new BadRequestException("Enter a valid email address");
        }

        // The public form always implies CAREER_PAGE - it's never worth
        // trusting a client-supplied source here the way staff-logged
        // applications (RecruitmentServiceImpl.apply) can be.
        Candidate candidate = candidateService.findOrCreate(settings.getCompanyId(), name, email,
                trimToNull(request.getApplicantPhone()), ApplicationSource.CAREER_PAGE,
                trimToNull(request.getResumeUrl()), trimToNull(request.getLinkedInUrl()), trimToNull(request.getPortfolioUrl()));

        // OFFER_REJECTED included alongside REJECTED/WITHDRAWN - see
        // RecruitmentServiceImpl.apply()'s identical exclusion list.
        if (applicationRepository.existsByJobPostingIdAndCandidateIdAndStatusNotIn(posting.getId(), candidate.getId(),
                java.util.List.of(com.businessos.enums.ApplicationStatus.REJECTED, com.businessos.enums.ApplicationStatus.WITHDRAWN,
                        com.businessos.enums.ApplicationStatus.OFFER_REJECTED))) {
            throw new BadRequestException("You have already applied for this position with that email");
        }

        Company companyRef = new Company();
        companyRef.setId(settings.getCompanyId());

        JobApplication application = JobApplication.builder()
                .jobPosting(posting)
                .company(companyRef)
                .candidate(candidate)
                .coverLetter(trimToNull(request.getCoverLetter()))
                .source(ApplicationSource.CAREER_PAGE)
                .status(ApplicationStatus.APPLIED)
                .build();
        applicationRepository.save(application);
        cvScoringService.scheduleAfterCommit(settings.getCompanyId(), application.getId());
        return ResponseEntity.ok(new ApplyResult("Application received - we'll be in touch"));
    }

    /**
     * Anonymous resume upload for the public apply form - storeFile() already
     * handles the unauthenticated case gracefully (a "guest_{uuid}" filename,
     * see LocalFileStorageService.persist()), so this is a thin pass-through
     * with no new validation logic. The returned URL is what the apply form
     * then sends back as resumeUrl - only a URL under our own /uploads/ path
     * is ever read for ATS scoring (see CvScoringService).
     */
    @PostMapping("/{slug}/upload-resume")
    public ResponseEntity<Map<String, String>> uploadResume(@PathVariable String slug,
                                                              @RequestParam("file") MultipartFile file) {
        requirePublished(slug);
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileStorageService.storeFile(file));
        return ResponseEntity.ok(response);
    }

    // ── Helpers ───────────────────────────────────────────────

    private CareerPageSettings requirePublished(String slug) {
        return settingsRepository.findBySlugIgnoreCase(slug)
                .filter(CareerPageSettings::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("Career page not found"));
    }

    private List<JobPosting> openPostings(Long companyId) {
        return jobPostingRepository.findByCompanyIdAndStatus(companyId, JobPostingStatus.OPEN).stream()
                .filter(p -> p.getDeadline() == null || !p.getDeadline().isBefore(LocalDate.now()))
                .toList();
    }

    private JobPosting requireOpenPosting(CareerPageSettings settings, Long jobId) {
        JobPosting posting = jobPostingRepository.findByIdAndCompanyId(jobId, settings.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (posting.getStatus() != JobPostingStatus.OPEN
                || (posting.getDeadline() != null && posting.getDeadline().isBefore(LocalDate.now()))) {
            throw new ResourceNotFoundException("This position is no longer open");
        }
        return posting;
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ── Public DTOs ───────────────────────────────────────────

    @Getter @Setter
    public static class CareerPageView {
        private String companyName;
        private String headline;
        private String about;
        private String brandColor;
        private List<JobCard> jobs;
    }

    @Getter @Setter
    public static class JobCard {
        private Long id;
        private String title;
        private String location;
        private String employmentType;
        private boolean remote;
        private LocalDate deadline;
        private String departmentName;

        static JobCard from(JobPosting p) {
            JobCard c = new JobCard();
            c.id = p.getId();
            c.title = p.getTitle() != null ? p.getTitle() : p.getJobTitle();
            c.location = p.getLocation();
            c.employmentType = p.getEmploymentType() != null ? p.getEmploymentType().name() : null;
            c.remote = Boolean.TRUE.equals(p.getRemote());
            c.deadline = p.getDeadline();
            c.departmentName = p.getDepartment() != null ? p.getDepartment().getName() : null;
            return c;
        }
    }

    @Getter @Setter
    public static class JobDetail extends JobCard {
        private String description;
        private String requirements;
        private String responsibilities;
        private BigDecimal salaryMin;
        private BigDecimal salaryMax;
        private Integer vacancies;

        static JobDetail from(JobPosting p) {
            JobDetail d = new JobDetail();
            d.setId(p.getId());
            d.setTitle(p.getTitle() != null ? p.getTitle() : p.getJobTitle());
            d.setLocation(p.getLocation());
            d.setEmploymentType(p.getEmploymentType() != null ? p.getEmploymentType().name() : null);
            d.setRemote(Boolean.TRUE.equals(p.getRemote()));
            d.setDeadline(p.getDeadline());
            d.setDepartmentName(p.getDepartment() != null ? p.getDepartment().getName() : null);
            d.description = p.getDescription();
            d.requirements = p.getRequirements();
            d.responsibilities = p.getResponsibilities();
            d.salaryMin = p.getSalaryMin();
            d.salaryMax = p.getSalaryMax();
            d.vacancies = p.getVacancies();
            return d;
        }
    }

    @Getter @Setter
    public static class ApplyRequest {
        private String applicantName;
        private String applicantEmail;
        private String applicantPhone;
        private String resumeUrl;
        private String coverLetter;
        private String linkedInUrl;
        private String portfolioUrl;
        /** Honeypot - must stay empty. */
        private String website;
    }

    public record ApplyResult(String message) {}
}
