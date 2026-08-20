package com.businessos.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class RecommendationResponse {
    //FOLLOW_UP, SLA_RISK, LICENSE_EXPIRY, OVERDUE_INVOICE
    private String type;
    // INFO, WARNING, CRITICAL
    private String severity;
    private String message;
    //Frontend route to act on the recommendation
    private String link;
}
