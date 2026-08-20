package com.businessos.modules.hrm.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PerformanceReviewRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    @NotNull
    private LocalDate reviewPeriodStart;
    @NotNull
    private LocalDate reviewPeriodEnd;
    @Min(1) @Max(10)
    private Integer scoreWorkQuality;
    @Min(1) @Max(10)
    private Integer scoreProductivity;
    @Min(1) @Max(10)
    private Integer scoreCommunication;
    @Min(1) @Max(10)
    private Integer scoreTeamwork;
    @Min(1) @Max(10)
    private Integer scoreInitiative;
    @Min(1) @Max(10)
    private Integer scorePunctuality;
    // Competencies were validated @Max(5) while the form has always been
    // labelled 1-10, so any score above 5 was rejected. Now 1-10 throughout.
    @Min(1) @Max(10)
    private Integer scoreLeadership;
    @Min(1) @Max(10)
    private Integer scoreProblemSolving;
    @Min(1) @Max(10)
    private Integer scoreInnovation;

    private String strengths;
    private String areasForImprovement;
    private String goalsForNextPeriod;
    private String comments;

    // ── Review outcome ────────────────────────────────────────────
    private String performanceLevel;
    private String promotionRecommendation;
    private String promotionReadiness;
    private String salaryIncrement;
    private String employmentStatusRecommendation;
    @Min(0) @Max(100)
    private Integer goalCompletionPercent;
    /** Comma-separated training topics. */
    private String trainingRecommendation;
    /** Comma-separated recognition awards. */
    private String recognition;
    /** JSON array of {title, progress} for the goal-tracking bars. */
    private String goals;
}
