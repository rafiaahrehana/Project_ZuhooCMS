package com.businessos.modules.hrm.performance;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class PerformanceMapper {

    /** A terminated employee's lazy proxy throws on any field access beyond its id - see TimesheetMapper.safeUser. */
    private static User safeUser(Employee emp) {
        if (emp == null) return null;
        try {
            return emp.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public static PerformanceReviewResponse toPerformanceReviewResponse(PerformanceReview pr) {
        Employee emp = pr.getEmployee();
        User empUser = safeUser(emp);
        Employee reviewer = pr.getReviewedBy();
        User reviewerUser = safeUser(reviewer);
        PerformanceReviewResponse r = new PerformanceReviewResponse();
        r.setId(pr.getId());
        r.setReviewPeriodStart(pr.getReviewPeriodStart());
        r.setReviewPeriodEnd(pr.getReviewPeriodEnd());
        r.setScoreWorkQuality(pr.getScoreWorkQuality());
        r.setScoreProductivity(pr.getScoreProductivity());
        r.setScoreCommunication(pr.getScoreCommunication());
        r.setScoreTeamwork(pr.getScoreTeamwork());
        r.setScoreInitiative(pr.getScoreInitiative());
        r.setScorePunctuality(pr.getScorePunctuality());
        r.setScoreLeadership(pr.getScoreLeadership());
        r.setScoreProblemSolving(pr.getScoreProblemSolving());
        r.setScoreInnovation(pr.getScoreInnovation());
        r.setOverallScore(pr.getOverallScore());
        r.setPerformanceLevel(pr.getPerformanceLevel());
        r.setPromotionRecommendation(pr.getPromotionRecommendation());
        r.setPromotionReadiness(pr.getPromotionReadiness());
        r.setSalaryIncrement(pr.getSalaryIncrement());
        r.setEmploymentStatusRecommendation(pr.getEmploymentStatusRecommendation());
        r.setGoalCompletionPercent(pr.getGoalCompletionPercent());
        r.setTrainingRecommendation(pr.getTrainingRecommendation());
        r.setRecognition(pr.getRecognition());
        r.setGoals(pr.getGoals());
        r.setStage(pr.getStage() != null ? pr.getStage().name() : null);
        r.setSelfAssessmentAt(pr.getSelfAssessmentAt());
        r.setSelfAssessmentBy(pr.getSelfAssessmentBy());
        r.setManagerReviewAt(pr.getManagerReviewAt());
        r.setManagerReviewBy(pr.getManagerReviewBy());
        r.setHrApprovalAt(pr.getHrApprovalAt());
        r.setHrApprovalBy(pr.getHrApprovalBy());
        r.setFinalApprovalAt(pr.getFinalApprovalAt());
        r.setFinalApprovalBy(pr.getFinalApprovalBy());
        r.setStrengths(pr.getStrengths());
        r.setAreasForImprovement(pr.getAreasForImprovement());
        r.setGoalsForNextPeriod(pr.getGoalsForNextPeriod());
        r.setComments(pr.getComments());
        r.setFinalised(pr.isFinalised());
        r.setEmployeeId(emp != null ? emp.getId() : null);
        r.setEmployeeName(empUser != null ? empUser.getFullName() : null);
        r.setReviewedById(reviewer != null ? reviewer.getId() : null);
        r.setReviewedByName(reviewerUser != null ? reviewerUser.getFullName() : null);
        r.setCreatedAt(pr.getCreatedAt());
        return r;

    }
}
