package com.businessos.modules.hrm.performance;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PerformanceReviewResponse {
    private Long id;
    private LocalDate reviewPeriodStart;
    private LocalDate reviewPeriodEnd;
    private Integer scoreWorkQuality;
    private Integer scoreProductivity;
    private Integer scoreCommunication;
    private Integer scoreTeamwork;
    private Integer scoreInitiative;
    private Integer scorePunctuality;
    private Double overallScore;
    private Integer scoreLeadership;
    private Integer scoreProblemSolving;
    private Integer scoreInnovation;
    private String strengths;
    private String areasForImprovement;
    private String goalsForNextPeriod;
    private String comments;
    private String performanceLevel;
    private String promotionRecommendation;
    private String promotionReadiness;
    private String salaryIncrement;
    private String employmentStatusRecommendation;
    private Integer goalCompletionPercent;
    private String trainingRecommendation;
    private String recognition;
    private String goals;

    // Approval chain
    private String stage;
    private LocalDateTime selfAssessmentAt;
    private String selfAssessmentBy;
    private LocalDateTime managerReviewAt;
    private String managerReviewBy;
    private LocalDateTime hrApprovalAt;
    private String hrApprovalBy;
    private LocalDateTime finalApprovalAt;
    private String finalApprovalBy;
    private boolean finalised;
    private Long employeeId;
    private String employeeName;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private String aiSummary;
}
