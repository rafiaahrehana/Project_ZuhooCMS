package com.businessos.enums;

public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    SHORTLISTED,
    INTERVIEW_SCHEDULED,
    INTERVIEWED,
    SELECTED,
    // Granular offer sub-pipeline, replacing a single OFFERED status - these
    // only change through JobOfferController's dedicated actions, never the
    // generic status endpoint, so an offer's state and the application's
    // state can't drift apart the way OFFERED-vs-declined-vs-REJECTED used to.
    OFFER_PENDING,
    OFFER_SENT,
    OFFER_ACCEPTED,
    OFFER_REJECTED,
    HIRED,
    REJECTED,
    WITHDRAWN
}
