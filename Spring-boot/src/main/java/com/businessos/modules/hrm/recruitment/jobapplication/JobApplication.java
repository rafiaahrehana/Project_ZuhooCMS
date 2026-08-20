package com.businessos.modules.hrm.recruitment.jobapplication;

import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;
import com.businessos.modules.hrm.recruitment.candidate.Candidate;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.enums.ApplicationSource;
import com.businessos.enums.ApplicationStatus;
import com.businessos.enums.AtsParseStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "job_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false) private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false) private Company company;

    // The person applying - see Candidate for name/email/phone/resume/links,
    // which live there now so one person can have several applications
    // without duplicating their contact details on each one.
    //
    // Not DB-NOT-NULL: Hibernate's ddl-auto=update adds new columns via a
    // plain ALTER, which Postgres rejects outright as NOT NULL against a
    // table with existing rows (there's no Flyway here to sequence
    // "add nullable -> backfill -> add constraint" across steps). Every
    // code path that creates a JobApplication sets this - see
    // RecruitmentServiceImpl.apply() and RecruitmentDataMigrationRunner,
    // which backfills it for pre-existing rows on boot.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id") private Candidate candidate;

    private String coverLetter;

    /** How this specific application arrived - can differ from Candidate.source (e.g. a second application via referral). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20) private ApplicationSource source;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDateTime interviewAt;
    private String interviewNotes;
    @Column(columnDefinition = "TEXT") private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id") private User reviewedBy;

    // Set once the application is hired — mirrors Lead.convertedClient/convertedAt in the CRM module.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_employee_id") private Employee convertedEmployee;

    private LocalDateTime convertedAt;

    // ── Candidate evaluation (0-100 each, all optional) ──────────
    // Per-application, not per-candidate: the same person can fit two
    // different roles differently, so a single global "candidate score"
    // would conflate fit-for-X with fit-for-Y. Weighted into overallScore
    // by RecruitmentServiceImpl.evaluate() - see that method for the weights.
    private Integer scoreEducation;
    private Integer scoreExperience;
    private Integer scoreTechnicalSkills;
    private Integer scoreInterview;
    private Integer scoreCommunication;
    private Double overallScore;

    // ── Automated ATS match (CvScoringService) ────────────────────
    // A signal alongside the manual scores above, never a replacement -
    // computed once at apply time from the resume file, when one of our own
    // uploads, against the posting's requiredSkills/preferredSkills/
    // minExperienceYears/minEducationLevel. See CvScoringService for the
    // weighting and AtsParseStatus for why a given application may never
    // get scored (no resume, unsupported format, no requirements set).
    private Integer atsScore;
    @Column(length = 500) private String atsMatchedRequiredSkills;
    @Column(length = 500) private String atsMissingRequiredSkills;
    @Column(length = 500) private String atsMatchedPreferredSkills;
    private Integer atsExtractedExperienceYears;
    private Boolean atsMeetsEducationRequirement;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20) private AtsParseStatus atsParseStatus = AtsParseStatus.PENDING;
    private Instant atsParsedAt;
}
