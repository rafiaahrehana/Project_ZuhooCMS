package com.businessos.modules.hrm.recruitment.kpi;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.ApplicationSource;
import com.businessos.enums.ApplicationStatus;
import com.businessos.enums.AtsParseStatus;
import com.businessos.enums.JobPostingStatus;
import com.businessos.modules.hrm.recruitment.candidate.CandidateRepository;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;
import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.modules.hrm.recruitment.offer.JobOffer;
import com.businessos.modules.hrm.recruitment.offer.JobOfferRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates entirely in Java over one fetch each of applications/postings/
 * offers - matches HrDashboardServiceImpl's live-aggregation approach, and
 * at this app's scale (dozens to low hundreds of applications per company)
 * is simpler and cheaper than N per-breakdown queries or native SQL, which
 * this codebase doesn't use anywhere.
 *
 * "Reached stage X" is inferred from an application's CURRENT status via
 * PIPELINE_ORDER, not from a stage-history audit trail (none exists) - a
 * candidate rejected after interview 2 still only shows as REJECTED today,
 * so REJECTED/WITHDRAWN are excluded from "reached" counts rather than
 * guessed at.
 *
 * Date filtering (from/to, both optional): defines the "applied in this
 * period" window on JobApplication.createdAt. Every derived figure -
 * candidates, funnel, source breakdown, job/recruiter tables, top
 * candidates, offer acceptance - is recomputed from that filtered set, so
 * the whole report stays internally consistent (an offer only counts if the
 * application it belongs to fell in the window). openPositions and
 * hiresThisMonth are deliberately NOT filtered - they're "right now" pulse
 * figures, not period activity, same distinction a live dashboard vs a
 * dated report would draw.
 *
 * minScore (optional) only narrows the Top Evaluated Candidates list, not
 * the rest of the report - most applications are never scored at all, so
 * treating an unset score as "excluded" everywhere else would silently drop
 * most of the pipeline out of the funnel/job/recruiter figures.
 */
@Service
@RequiredArgsConstructor
public class RecruitmentKpiServiceImpl implements RecruitmentKpiService {

    private final JobApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateRepository candidateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    private static final Map<ApplicationStatus, Integer> PIPELINE_ORDER = Map.ofEntries(
        Map.entry(ApplicationStatus.APPLIED, 0),
        Map.entry(ApplicationStatus.SCREENING, 1),
        Map.entry(ApplicationStatus.SHORTLISTED, 2),
        Map.entry(ApplicationStatus.INTERVIEW_SCHEDULED, 3),
        Map.entry(ApplicationStatus.INTERVIEWED, 4),
        Map.entry(ApplicationStatus.SELECTED, 5),
        Map.entry(ApplicationStatus.OFFER_PENDING, 6),
        Map.entry(ApplicationStatus.OFFER_SENT, 6),
        Map.entry(ApplicationStatus.OFFER_REJECTED, 6),
        Map.entry(ApplicationStatus.OFFER_ACCEPTED, 7),
        Map.entry(ApplicationStatus.HIRED, 8)
    );
    private static final int INTERVIEW_ORDER = PIPELINE_ORDER.get(ApplicationStatus.INTERVIEW_SCHEDULED);

    @Override
    @Transactional(readOnly = true)
    public RecruitmentKpiResponse getSummary(LocalDate from, LocalDate to, Double minScore) {
        authorizationService.checkPermission(PermissionCode.RECRUITMENT_REPORT_VIEW);
        Long companyId = requireCompanyId();

        List<JobApplication> allApplications = applicationRepository.findByCompanyId(companyId);
        List<JobPosting> postings = jobPostingRepository.findByCompanyId(companyId);
        List<JobOffer> allOffers = jobOfferRepository.findByCompanyId(companyId);

        List<JobApplication> applications = filterByAppliedDate(allApplications, from, to);
        Set<Long> inRangeApplicationIds = applications.stream().map(JobApplication::getId).collect(Collectors.toSet());
        List<JobOffer> offers = allOffers.stream()
            .filter(o -> o.getJobApplication() != null && inRangeApplicationIds.contains(o.getJobApplication().getId()))
            .toList();

        List<JobApplication> hired = applications.stream()
            .filter(a -> a.getStatus() == ApplicationStatus.HIRED && a.getConvertedAt() != null)
            .toList();
        // "This month" hires are a live pulse figure and stay real-time even
        // when a from/to filter narrows everything else - it uses allApplications,
        // not the filtered set, on purpose.
        List<JobApplication> allHired = allApplications.stream()
            .filter(a -> a.getStatus() == ApplicationStatus.HIRED && a.getConvertedAt() != null)
            .toList();
        long reachedInterview = applications.stream().filter(this::reachedInterview).count();
        YearMonth thisMonth = YearMonth.now();

        return RecruitmentKpiResponse.builder()
            .openPositions(postings.stream().filter(p -> p.getStatus() == JobPostingStatus.OPEN).count())
            .totalCandidates(applications.stream()
                .map(a -> a.getCandidate() != null ? a.getCandidate().getId() : null)
                .filter(java.util.Objects::nonNull).distinct().count())
            .totalApplications(applications.size())
            .hiresThisMonth(allHired.stream().filter(a -> YearMonth.from(a.getConvertedAt()).equals(thisMonth)).count())
            .hiresTotal(hired.size())
            .avgTimeToHireDays(avgDays(hired, a -> a.getCreatedAt(), JobApplication::getConvertedAt))
            .avgTimeToFillDays(avgTimeToFillDays(hired))
            .applicationToInterviewRate(rate(reachedInterview, applications.size()))
            .interviewToHireRate(rate(hired.size(), reachedInterview))
            .offerAcceptanceRate(offerAcceptanceRate(offers))
            .avgAtsMatchScore(avgAtsMatchScore(applications))
            .funnel(funnel(applications))
            .sourceBreakdown(sourceBreakdown(applications))
            .jobKpis(jobKpis(postings, applications, offers))
            .recruiterKpis(recruiterKpis(postings, applications, offers))
            .topCandidates(topCandidates(applications, minScore))
            .build();
    }

    private List<JobApplication> filterByAppliedDate(List<JobApplication> apps, LocalDate from, LocalDate to) {
        if (from == null && to == null) return apps;
        return apps.stream().filter(a -> {
            if (a.getCreatedAt() == null) return false;
            LocalDate appliedOn = a.getCreatedAt().toLocalDate();
            if (from != null && appliedOn.isBefore(from)) return false;
            if (to != null && appliedOn.isAfter(to)) return false;
            return true;
        }).toList();
    }

    private boolean reachedInterview(JobApplication a) {
        Integer order = PIPELINE_ORDER.get(a.getStatus());
        return order != null && order >= INTERVIEW_ORDER;
    }

    private long count(List<JobApplication> apps, ApplicationStatus status) {
        return apps.stream().filter(a -> a.getStatus() == status).count();
    }

    private long inOfferSubPipeline(List<JobApplication> apps) {
        return apps.stream().filter(a -> {
            Integer order = PIPELINE_ORDER.get(a.getStatus());
            return order != null && order >= 6 && order <= 7;
        }).count();
    }

    private Double rate(long numerator, long denominator) {
        return denominator == 0 ? null : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    private Double avgDays(List<JobApplication> apps,
                            java.util.function.Function<JobApplication, LocalDateTime> from,
                            java.util.function.Function<JobApplication, LocalDateTime> to) {
        if (apps.isEmpty()) return null;
        OptionalDouble avg = apps.stream()
            .filter(a -> from.apply(a) != null && to.apply(a) != null)
            .mapToLong(a -> ChronoUnit.DAYS.between(from.apply(a).toLocalDate(), to.apply(a).toLocalDate()))
            .average();
        return avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null;
    }

    private Double avgTimeToFillDays(List<JobApplication> hired) {
        List<Long> days = new ArrayList<>();
        for (JobApplication a : hired) {
            JobPosting posting = a.getJobPosting();
            if (posting == null || posting.getCreatedAt() == null || a.getConvertedAt() == null) continue;
            days.add(ChronoUnit.DAYS.between(posting.getCreatedAt().toLocalDate(), a.getConvertedAt().toLocalDate()));
        }
        if (days.isEmpty()) return null;
        return Math.round(days.stream().mapToLong(Long::longValue).average().orElse(0) * 10) / 10.0;
    }

    private Double offerAcceptanceRate(List<JobOffer> offers) {
        long accepted = offers.stream().filter(o -> o.getStatus() == JobOffer.Status.ACCEPTED).count();
        long declined = offers.stream().filter(o -> o.getStatus() == JobOffer.Status.DECLINED).count();
        return rate(accepted, accepted + declined);
    }

    /** Mean CvScoringService.atsScore over applications that were actually scored - unscored/failed/not-applicable ones don't drag the average down or inflate it. */
    private Double avgAtsMatchScore(List<JobApplication> applications) {
        OptionalDouble avg = applications.stream()
            .filter(a -> a.getAtsParseStatus() == AtsParseStatus.SUCCESS && a.getAtsScore() != null)
            .mapToInt(JobApplication::getAtsScore)
            .average();
        return avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null;
    }

    private List<RecruitmentKpiResponse.FunnelStage> funnel(List<JobApplication> applications) {
        long applied = count(applications, ApplicationStatus.APPLIED);
        long screening = count(applications, ApplicationStatus.SCREENING) + count(applications, ApplicationStatus.SHORTLISTED);
        long interview = count(applications, ApplicationStatus.INTERVIEW_SCHEDULED)
            + count(applications, ApplicationStatus.INTERVIEWED) + count(applications, ApplicationStatus.SELECTED);
        long offer = inOfferSubPipeline(applications);
        long hired = count(applications, ApplicationStatus.HIRED);
        return List.of(
            stage("Applied", applied), stage("Screening", screening), stage("Interview", interview),
            stage("Offer", offer), stage("Hired", hired));
    }

    private RecruitmentKpiResponse.FunnelStage stage(String name, long count) {
        return RecruitmentKpiResponse.FunnelStage.builder().stage(name).count(count).build();
    }

    private List<RecruitmentKpiResponse.SourceSlice> sourceBreakdown(List<JobApplication> applications) {
        long total = applications.size();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (JobApplication a : applications) {
            String key = a.getSource() != null ? a.getSource().name() : "UNKNOWN";
            counts.merge(key, 1L, Long::sum);
        }
        return counts.entrySet().stream()
            .map(e -> RecruitmentKpiResponse.SourceSlice.builder()
                .source(e.getKey())
                .count(e.getValue())
                .percent(total == 0 ? 0 : Math.round(e.getValue() * 1000.0 / total) / 10.0)
                .build())
            .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
            .toList();
    }

    private List<RecruitmentKpiResponse.JobKpi> jobKpis(List<JobPosting> postings, List<JobApplication> applications, List<JobOffer> offers) {
        Map<Long, List<JobApplication>> appsByPosting = applications.stream()
            .filter(a -> a.getJobPosting() != null)
            .collect(Collectors.groupingBy(a -> a.getJobPosting().getId()));
        Map<Long, List<JobOffer>> offersByPosting = offers.stream()
            .filter(o -> o.getJobApplication() != null && o.getJobApplication().getJobPosting() != null)
            .collect(Collectors.groupingBy(o -> o.getJobApplication().getJobPosting().getId()));

        List<RecruitmentKpiResponse.JobKpi> result = new ArrayList<>();
        for (JobPosting posting : postings) {
            List<JobApplication> apps = appsByPosting.getOrDefault(posting.getId(), List.of());
            List<JobOffer> postingOffers = offersByPosting.getOrDefault(posting.getId(), List.of());
            List<JobApplication> hired = apps.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED && a.getConvertedAt() != null)
                .toList();
            result.add(RecruitmentKpiResponse.JobKpi.builder()
                .jobPostingId(posting.getId())
                .jobTitle(posting.getTitle())
                .status(posting.getStatus().name())
                .applications(apps.size())
                .shortlisted(count(apps, ApplicationStatus.SHORTLISTED))
                .interviews(count(apps, ApplicationStatus.INTERVIEW_SCHEDULED) + count(apps, ApplicationStatus.INTERVIEWED)
                    + count(apps, ApplicationStatus.SELECTED))
                .offers(inOfferSubPipeline(apps))
                .hired(hired.size())
                .timeToFillDays(avgTimeToFillDays(hired))
                .offerAcceptanceRate(offerAcceptanceRate(postingOffers))
                .avgAtsMatchScore(avgAtsMatchScore(apps))
                .build());
        }
        return result;
    }

    private List<RecruitmentKpiResponse.RecruiterKpi> recruiterKpis(List<JobPosting> postings, List<JobApplication> applications, List<JobOffer> offers) {
        Map<Long, List<JobPosting>> postingsByRecruiter = postings.stream()
            .filter(p -> p.getAssignedRecruiter() != null)
            .collect(Collectors.groupingBy(p -> p.getAssignedRecruiter().getId()));

        List<RecruitmentKpiResponse.RecruiterKpi> result = new ArrayList<>();
        for (Map.Entry<Long, List<JobPosting>> entry : postingsByRecruiter.entrySet()) {
            List<JobPosting> recruiterPostings = entry.getValue();
            java.util.Set<Long> postingIds = recruiterPostings.stream().map(JobPosting::getId).collect(Collectors.toSet());
            List<JobApplication> apps = applications.stream()
                .filter(a -> a.getJobPosting() != null && postingIds.contains(a.getJobPosting().getId()))
                .toList();
            List<JobOffer> recruiterOffers = offers.stream()
                .filter(o -> o.getJobApplication() != null && o.getJobApplication().getJobPosting() != null
                    && postingIds.contains(o.getJobApplication().getJobPosting().getId()))
                .toList();
            List<JobApplication> hired = apps.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED && a.getConvertedAt() != null)
                .toList();
            com.businessos.modules.hrm.employee.Employee recruiter = recruiterPostings.get(0).getAssignedRecruiter();

            result.add(RecruitmentKpiResponse.RecruiterKpi.builder()
                .recruiterId(entry.getKey())
                .recruiterName(recruiter.getUser() != null ? recruiter.getUser().getFullName() : null)
                .jobsManaged(recruiterPostings.size())
                .applications(apps.size())
                .shortlisted(count(apps, ApplicationStatus.SHORTLISTED))
                .interviews(count(apps, ApplicationStatus.INTERVIEW_SCHEDULED) + count(apps, ApplicationStatus.INTERVIEWED)
                    + count(apps, ApplicationStatus.SELECTED))
                .offers(inOfferSubPipeline(apps))
                .hires(hired.size())
                .avgTimeToHireDays(avgDays(hired, JobApplication::getCreatedAt, JobApplication::getConvertedAt))
                .offerAcceptanceRate(offerAcceptanceRate(recruiterOffers))
                .avgAtsMatchScore(avgAtsMatchScore(apps))
                .build());
        }
        result.sort((a, b) -> Long.compare(b.getHires(), a.getHires()));
        return result;
    }

    private static final int TOP_CANDIDATES_LIMIT = 10;
    // A minScore threshold already bounds the result set meaningfully - a
    // recruiter asking "who's above 90?" wants everyone above 90, not just
    // the first 10 of them.
    private static final int TOP_CANDIDATES_LIMIT_WITH_MIN_SCORE = 50;

    private List<RecruitmentKpiResponse.TopCandidate> topCandidates(List<JobApplication> applications, Double minScore) {
        int limit = minScore != null ? TOP_CANDIDATES_LIMIT_WITH_MIN_SCORE : TOP_CANDIDATES_LIMIT;
        return applications.stream()
            .filter(a -> a.getOverallScore() != null && a.getCandidate() != null)
            .filter(a -> minScore == null || a.getOverallScore() >= minScore)
            .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
            .limit(limit)
            .map(a -> RecruitmentKpiResponse.TopCandidate.builder()
                .applicationId(a.getId())
                .candidateName(a.getCandidate().getName())
                .jobTitle(a.getJobPosting() != null ? a.getJobPosting().getTitle() : null)
                .overallScore(a.getOverallScore())
                .build())
            .toList();
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
