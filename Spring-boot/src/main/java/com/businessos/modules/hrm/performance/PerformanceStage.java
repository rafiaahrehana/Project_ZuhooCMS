package com.businessos.modules.hrm.performance;

/**
 * The approval chain a performance review walks through.
 *
 * This sits alongside ReviewStatus rather than replacing it: ReviewStatus
 * (DRAFT/SUBMITTED/ACKNOWLEDGED) describes the employee-facing state, while the
 * stage tracks who still has to sign off. Reaching COMPLETED is what sets the
 * review's `finalised` flag, so there is exactly one way a review becomes final.
 */
public enum PerformanceStage {
    SELF_ASSESSMENT,
    MANAGER_REVIEW,
    HR_APPROVAL,
    FINAL_APPROVAL,
    COMPLETED;

    /** Next stage in the chain, or null once COMPLETED. */
    public PerformanceStage next() {
        return switch (this) {
            case SELF_ASSESSMENT -> MANAGER_REVIEW;
            case MANAGER_REVIEW  -> HR_APPROVAL;
            case HR_APPROVAL     -> FINAL_APPROVAL;
            case FINAL_APPROVAL  -> COMPLETED;
            case COMPLETED       -> null;
        };
    }
}
