package com.businessos.modules.hrm.recruitment.interview;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One interview round for a job application: the schedule (round, time, mode,
 * interviewer) and, once held, the interviewer's feedback (rating,
 * strengths/concerns, hire recommendation).
 */
@Entity
@Table(name = "recruitment_interviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview extends BaseEntity {

    public enum Round { SCREENING, TECHNICAL, HR, FINAL }
    public enum Mode { ONSITE, VIDEO, PHONE }
    public enum Status { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }
    public enum Recommendation { STRONG_HIRE, HIRE, NEUTRAL, NO_HIRE, STRONG_NO_HIRE }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Round round = Round.SCREENING;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Mode mode = Mode.VIDEO;

    private String meetingLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private Employee interviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SCHEDULED;

    // ── Feedback (set when the round completes) ───────────────
    /** 1-5. */
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String concerns;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Recommendation recommendation;

    private LocalDateTime feedbackAt;
}
