package com.businessos.modules.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder
public class PlatformSummaryResponse {

    // Companies by lifecycle status
    long totalCompanies;
    long activeCompanies;
    long trialCompanies;
    long suspendedCompanies;
    long pendingVerificationCompanies;

    // Trials whose subscription window ends within the next 7 days
    long trialsExpiringWithin7Days;

    // Companies per catalog plan - dynamic, since Super Admin can add/remove plans
    // at runtime (see SubscriptionPlanDefinition). Replaces the old fixed
    // freePlanCompanies/starterPlanCompanies/proPlanCompanies/enterprisePlanCompanies fields.
    List<PlanCompanyCount> companiesByPlan;

    // SaaS staff accounts (all platform roles)
    long totalPlatformUsers;

    // Recorded subscription revenue (SUM of SubscriptionHistory.amountPaid)
    BigDecimal totalRevenue;
    BigDecimal revenueThisMonth;
}
