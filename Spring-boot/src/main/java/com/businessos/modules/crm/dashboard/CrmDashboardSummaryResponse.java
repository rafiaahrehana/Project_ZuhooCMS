package com.businessos.modules.crm.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmDashboardSummaryResponse {
    private BigDecimal pipelineValue;
    private BigDecimal wonThisMonth;
    private long qualifiedLeadsCount;
    private double conversionRate; // percent, 0-100
    private List<UpcomingFollowUp> upcomingFollowUps;

    private long totalClients;
    private long totalLeads;
    private long totalOpportunities;
    private long openOpportunitiesCount;
    private long wonCount;
    private BigDecimal wonValue;
    private long lostCount;
    private BigDecimal lostValue;

    /** Open deals per stage - the spec's funnel: count, value, share of pipeline. */
    private List<StageSlice> stageFunnel;
    /** Where leads come from, largest source first. */
    private List<SourceSlice> leadSources;
    /** Most recently touched deals. */
    private List<DealItem> recentDeals;

    @Data
    @Builder
    public static class StageSlice {
        private String stage;
        private long count;
        private BigDecimal value;
        /** Share of open pipeline VALUE, whole percent. */
        private int percent;
    }

    @Data
    @Builder
    public static class SourceSlice {
        private String source;
        private long count;
    }

    @Data
    @Builder
    public static class DealItem {
        private Long id;
        private String name;
        private String clientName;
        private BigDecimal amount;
        private String stage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingFollowUp {
        private Long activityId;
        private String subject;
        private LocalDateTime followUpAt;
        private String relatedName;
    }
}
