package com.businessos.modules.hrm.performance;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "performance_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerformanceReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id", nullable = false)
    private Employee reviewedBy;

    private LocalDate reviewPeriodStart;
    private LocalDate reviewPeriodEnd;

    // Competency scores (1-10)
    private Integer scoreWorkQuality;
    private Integer scoreProductivity;
    private Integer scoreCommunication;
    private Integer scoreTeamwork;
    // Retained so historical reviews keep their value; no longer on the form.
    private Integer scoreInitiative;
    private Integer scorePunctuality;
    private Integer scoreLeadership;
    private Integer scoreProblemSolving;
    private Integer scoreInnovation;
    private Double  overallScore;

    // ── Review outcome ────────────────────────────────────────────
    // Every column below is nullable on purpose: spring.jpa.hibernate.ddl-auto
    // is `update`, and adding a NOT NULL column to a table that already holds
    // rows makes the schema update fail at boot.

    /** Outstanding / Exceeds Expectations / Meets Expectations / Needs Improvement. */
    @Column(length = 40)
    private String performanceLevel;

    /** HIGHLY_RECOMMENDED / RECOMMENDED / NEEDS_IMPROVEMENT / NOT_RECOMMENDED. */
    @Column(length = 40)
    private String promotionRecommendation;

    /** High / Medium / Low — drives the readiness summary card. */
    @Column(length = 20)
    private String promotionReadiness;

    /** Free text so "No Increment" and "Custom" are as expressible as "12%". */
    @Column(length = 40)
    private String salaryIncrement;

    /** PROMOTE / RETAIN / PIP / TERMINATE. */
    @Column(length = 40)
    private String employmentStatusRecommendation;

    /** Percentage of the period's goals completed (0-100). */
    private Integer goalCompletionPercent;

    /** Comma-separated training topics. */
    @Column(columnDefinition = "TEXT")
    private String trainingRecommendation;

    /** Comma-separated recognition awards. */
    @Column(columnDefinition = "TEXT")
    private String recognition;

    /** JSON array of {title, progress} — goal tracking bars. */
    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String areasForImprovement;

    @Column(columnDefinition = "TEXT")
    private String goalsForNextPeriod;

    @Column(columnDefinition = "TEXT")
    private String managerComments;

    @Column(columnDefinition = "TEXT")
    private String employeeComments;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.DRAFT;

    private LocalDate reviewDate;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Builder.Default
    private boolean finalised = false;

    // ── Approval chain ────────────────────────────────────────────
    // Actor names are stored as text rather than as user FKs on purpose: this
    // is an audit trail, and it should still read correctly if the user is
    // later renamed, deactivated or removed.

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PerformanceStage stage = PerformanceStage.SELF_ASSESSMENT;

    private java.time.LocalDateTime selfAssessmentAt;
    private String selfAssessmentBy;

    private java.time.LocalDateTime managerReviewAt;
    private String managerReviewBy;

    private java.time.LocalDateTime hrApprovalAt;
    private String hrApprovalBy;

    private java.time.LocalDateTime finalApprovalAt;
    private String finalApprovalBy;

    // Set once PerformanceReviewOverdueScheduler notifies the reviewer this
    // review is overdue (reviewPeriodEnd has passed and it's still not
    // finalised). A "review every 6 months" policy had nothing in the system
    // enforcing or even flagging it - entirely tracked outside the app.
    private java.time.LocalDateTime overdueReminderSentAt;
}
