package com.businessos.modules.hrm.recruitment.interview;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.ApplicationStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interview scheduling + feedback for job applications.
 *
 * Application status rides along automatically: scheduling the first round
 * moves an early-stage application to INTERVIEW_SCHEDULED, and completing the
 * last outstanding round moves it to INTERVIEWED. Uses the APPLICATION_*
 * permission codes - interviews are part of working an application.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment/interviews")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class InterviewController {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;
    private final EmailBranding emailBranding;

    // ── Read ──────────────────────────────────────────────────

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<InterviewResponse>> list(
            @RequestParam(required = false) Interview.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        Long companyId = requireCompanyId();
        Page<Interview> result = status != null
            ? interviewRepository.findByCompanyIdAndStatusOrderByScheduledAtAsc(companyId, status, PageRequest.of(page, size))
            : interviewRepository.findByCompanyIdOrderByScheduledAtDesc(companyId, PageRequest.of(page, size));
        return ResponseEntity.ok(result.map(InterviewResponse::from));
    }

    @GetMapping("/application/{applicationId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InterviewResponse>> forApplication(@PathVariable Long applicationId) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        requireApplication(applicationId); // tenant check
        return ResponseEntity.ok(interviewRepository.findByJobApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream().map(InterviewResponse::from).toList());
    }

    // ── Schedule / reschedule ─────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<InterviewResponse> schedule(@RequestBody InterviewRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Long companyId = requireCompanyId();
        JobApplication application = requireApplication(request.getJobApplicationId());
        if (application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application is closed - reopen it before scheduling interviews");
        }
        if (request.getScheduledAt() == null) {
            throw new BadRequestException("Interview date/time is required");
        }
        if (request.getInterviewerId() == null) {
            throw new BadRequestException("An interviewer is required");
        }

        Company companyRef = new Company();
        companyRef.setId(companyId);

        Interview interview = Interview.builder()
                .company(companyRef)
                .jobApplication(application)
                .round(request.getRound() != null ? request.getRound() : Interview.Round.SCREENING)
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .mode(request.getMode() != null ? request.getMode() : Interview.Mode.VIDEO)
                .meetingLink(request.getMeetingLink())
                .interviewer(resolveInterviewer(request.getInterviewerId(), companyId))
                .build();
        interview = interviewRepository.save(interview);

        // First scheduled round pulls the application forward in the funnel.
        if (application.getStatus() == ApplicationStatus.APPLIED
                || application.getStatus() == ApplicationStatus.SCREENING
                || application.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        }

        // Previously nothing told the candidate an interview had been booked -
        // they only found out if someone called them, since candidates aren't
        // platform users and get no in-app notification.
        try {
            Company fullCompany = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            EmailBranding.Data branding = emailBranding.from(fullCompany);
            String details = "Round: " + interview.getRound()
                + " | When: " + interview.getScheduledAt()
                + " | Mode: " + interview.getMode()
                + (interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank()
                    ? " | Link: " + interview.getMeetingLink() : "");
            emailService.sendInterviewScheduledEmail(
                application.getCandidate().getEmail(), application.getCandidate().getName(), details, branding);
        } catch (Exception ex) {
            log.warn("Interview scheduled email failed (interview still booked): {}", ex.getMessage());
        }

        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<InterviewResponse> reschedule(@PathVariable Long id, @RequestBody InterviewRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Interview interview = requireInterview(id);
        if (interview.getStatus() != Interview.Status.SCHEDULED) {
            throw new BadRequestException("Only a scheduled interview can be edited");
        }
        if (request.getRound() != null) interview.setRound(request.getRound());
        if (request.getScheduledAt() != null) interview.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) interview.setDurationMinutes(request.getDurationMinutes());
        if (request.getMode() != null) interview.setMode(request.getMode());
        if (request.getMeetingLink() != null) interview.setMeetingLink(request.getMeetingLink());
        if (request.getInterviewerId() != null) {
            interview.setInterviewer(resolveInterviewer(request.getInterviewerId(), requireCompanyId()));
        }
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    // ── Outcomes ──────────────────────────────────────────────

    @PatchMapping("/{id}/feedback")
    @Transactional
    public ResponseEntity<InterviewResponse> feedback(@PathVariable Long id, @RequestBody FeedbackRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Interview interview = requireInterview(id);
        if (interview.getStatus() == Interview.Status.CANCELLED) {
            throw new BadRequestException("A cancelled interview cannot take feedback");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        interview.setRating(request.getRating());
        interview.setStrengths(request.getStrengths());
        interview.setConcerns(request.getConcerns());
        interview.setRecommendation(request.getRecommendation());
        interview.setFeedbackAt(LocalDateTime.now());
        interview.setStatus(request.isNoShow() ? Interview.Status.NO_SHOW : Interview.Status.COMPLETED);

        // Last outstanding round done -> the application has been interviewed.
        // A no-show means the interview didn't actually happen, so it doesn't
        // count as one - the recruiter decides what happens next (reschedule,
        // reject, ...) rather than the system pretending it went ahead.
        JobApplication application = interview.getJobApplication();
        if (!request.isNoShow()
                && application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                && !interviewRepository.existsByJobApplicationIdAndStatus(application.getId(), Interview.Status.SCHEDULED)) {
            application.setStatus(ApplicationStatus.INTERVIEWED);
        }
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<InterviewResponse> cancel(@PathVariable Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Interview interview = requireInterview(id);
        if (interview.getStatus() != Interview.Status.SCHEDULED) {
            throw new BadRequestException("Only a scheduled interview can be cancelled");
        }
        interview.setStatus(Interview.Status.CANCELLED);

        // If this was the last outstanding round, don't leave the application
        // stuck at INTERVIEW_SCHEDULED with nothing actually scheduled - drop
        // it back to SHORTLISTED so a fresh round can be booked.
        JobApplication application = interview.getJobApplication();
        if (application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                && !interviewRepository.existsByJobApplicationIdAndStatus(application.getId(), Interview.Status.SCHEDULED)) {
            application.setStatus(ApplicationStatus.SHORTLISTED);
        }
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Employee resolveInterviewer(Long interviewerId, Long companyId) {
        if (interviewerId == null) return null;
        return employeeRepository.findByIdAndCompanyId(interviewerId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found: " + interviewerId));
    }

    private Interview requireInterview(Long id) {
        return interviewRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + id));
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
    public static class InterviewRequest {
        private Long jobApplicationId;
        private Interview.Round round;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private Interview.Mode mode;
        private String meetingLink;
        private Long interviewerId;
    }

    @Getter @Setter
    public static class FeedbackRequest {
        private Integer rating;
        private String strengths;
        private String concerns;
        private Interview.Recommendation recommendation;
        private boolean noShow;
    }

    @Getter @Setter
    public static class InterviewResponse {
        private Long id;
        private Long jobApplicationId;
        private String applicantName;
        private String jobTitle;
        private String applicationStatus;
        private Interview.Round round;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private Interview.Mode mode;
        private String meetingLink;
        private Long interviewerId;
        private String interviewerName;
        private Interview.Status status;
        private Integer rating;
        private String strengths;
        private String concerns;
        private Interview.Recommendation recommendation;
        private LocalDateTime feedbackAt;

        static InterviewResponse from(Interview i) {
            InterviewResponse r = new InterviewResponse();
            r.id = i.getId();
            r.jobApplicationId = i.getJobApplication().getId();
            r.applicantName = i.getJobApplication().getCandidate() != null ? i.getJobApplication().getCandidate().getName() : null;
            r.jobTitle = i.getJobApplication().getJobPosting() != null
                    ? i.getJobApplication().getJobPosting().getTitle() : null;
            r.applicationStatus = i.getJobApplication().getStatus() != null
                    ? i.getJobApplication().getStatus().name() : null;
            r.round = i.getRound();
            r.scheduledAt = i.getScheduledAt();
            r.durationMinutes = i.getDurationMinutes();
            r.mode = i.getMode();
            r.meetingLink = i.getMeetingLink();
            if (i.getInterviewer() != null) {
                r.interviewerId = i.getInterviewer().getId();
                r.interviewerName = i.getInterviewer().getFullName();
            }
            r.status = i.getStatus();
            r.rating = i.getRating();
            r.strengths = i.getStrengths();
            r.concerns = i.getConcerns();
            r.recommendation = i.getRecommendation();
            r.feedbackAt = i.getFeedbackAt();
            return r;
        }
    }
}
